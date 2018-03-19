package jcrbox.springboot;

import static jcrbox.springboot.TestJcrStructure.Nodes.CUSTOMER;
import static jcrbox.springboot.TestJcrStructure.Nodes.INVOICE;
import static jcrbox.springboot.TestJcrStructure.Properties.NAME;
import static jcrbox.springboot.TestJcrStructure.Properties.ORDER_DATE;
import static jcrbox.springboot.TestJcrStructure.Properties.STATUS;
import static jcrbox.springboot.TestJcrStructure.Properties.VERIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModel;

import org.apache.commons.io.IOUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.modeshape.jcr.JcrRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jcrbox.Jcr;
import jcrbox.WithNode;
import jcrbox.fp.JcrConsumer;
import jcrbox.fp.JcrFunction;
import jcrbox.query.JcrQuery;
import jcrbox.query.JcrResult;
import jcrbox.query.JcrRow;
import jcrbox.query.QueryBuilder;
import jcrbox.query.QueryParameter;
import jcrbox.springboot.TestJcrStructure.InvoiceStatus;
import jcrbox.springboot.TestJcrStructure.Queries.CreatedInvoices;
import jcrbox.springboot.TestJcrStructure.Queries.CustomerWithInvoiceBefore;
import jcrbox.springboot.TestJcrStructure.Queries.InvoicesByCustomer;
import jcrbox.springboot.TestJcrStructure.Queries.VerifiedCustomerInvoices;
import jcrbox.springboot.TestJcrStructure.Queries.VerifiedCustomers;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = "jcr.repository-parameters.name=test-repo", classes = JcrTest.Application.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JcrTest {

    private @Autowired Jcr jcr;

    @Test
    public void jcrConfiguration() throws RepositoryException {
        assertNotNull(jcr);
        assertThat(jcr.session.getRepository()).isInstanceOf(JcrRepository.class).satisfies(repo -> {
            assertThat(JcrRepository.class.cast(repo).getName()).isEqualTo("test-repo");
        });
        final NodeTypeManager nodeTypeManager = jcr.session.getWorkspace().getNodeTypeManager();
        assertTrue(nodeTypeManager.hasNodeType(INVOICE.nodeName()));
        assertTrue(nodeTypeManager.hasNodeType(CUSTOMER.nodeName()));
        assertTrue(jcr.hasNode(JcrQuery.path(VerifiedCustomers.class), NodeType.NT_QUERY));
        assertTrue(jcr.hasNode(JcrQuery.path(VerifiedCustomerInvoices.class), NodeType.NT_QUERY));
        assertTrue(jcr.hasNode(JcrQuery.path(CreatedInvoices.class), NodeType.NT_QUERY));
        assertTrue(jcr.hasNode(JcrQuery.path(CustomerWithInvoiceBefore.class), NodeType.NT_QUERY));
    }

    @Test
    public void jcrOperations() throws RepositoryException, IOException {
        final WithNode customers = jcr.withRoot().next("customers");
        assertNotNull(customers);
        assertNotNull(customers.getTarget());
        assertEquals("nt:unstructured", customers.getTarget().getPrimaryNodeType().getName());

        final String customerNumber = Long.toString(1);

        final WithNode customer = customers.next(customerNumber, CUSTOMER).set(VERIFIED, vf -> null);

        assertEquals(customerNumber, customer.getTarget().getName());
        assertTrue(CUSTOMER.isPrimaryNodeTypeOf(customer.getTarget()));
        assertFalse(customer.getTarget().hasProperty(VERIFIED.propertyName()));

        customer.set(VERIFIED, vf -> vf.createValue(true));
        final Property verified = customer.get(VERIFIED);
        assertEquals(PropertyType.BOOLEAN, verified.getType());
        assertTrue(verified.getValue().getBoolean());

        final String orderId = "foo123";
        final ZonedDateTime orderTime = ZonedDateTime.now().minusDays(1);
        final WithNode invoice = customer.next(orderId, INVOICE).set(TestJcrStructure.Properties.ORDER_DATE,
            vf -> vf.createValue(GregorianCalendar.from(orderTime)));

        assertNotNull(invoice);
        assertNotNull(invoice.getTarget());
        assertTrue(INVOICE.isPrimaryNodeTypeOf(invoice.getTarget()));
        final Property sessionDate = invoice.get(ORDER_DATE);
        assertNotNull(sessionDate);
        assertThat(sessionDate.getDate()).isNotNull()
            .satisfies(cal -> assertThat(cal).isEqualByComparingTo(GregorianCalendar.from(orderTime)));

        final Property status = invoice.get(STATUS);
        assertNotNull(status);
        assertSame(InvoiceStatus.CREATED, InvoiceStatus.valueOf(status.getString()));
        Supplier<InputStream> invoiceData = () -> new ByteArrayInputStream("BLAHBLAHBLAHBLAH".getBytes());
        invoice.data(invoiceData);
        final Binary data = invoice.getTarget().getProperty(Property.JCR_DATA).getBinary();
        assertTrue(data.getSize() > 0);
        assertTrue(IOUtils.contentEquals(invoiceData.get(), data.getStream()));

        // add another invoice
        customer.next(orderId + "x", INVOICE)
            .set(ORDER_DATE, vf -> vf.createValue(GregorianCalendar.from(ZonedDateTime.now()))).data(invoiceData);

        jcr.session.save();
        assertFalse(jcr.session.hasPendingChanges());
    }

    @Test
    public void queryBasic() throws RepositoryException {
        assumeTrue(jcr.hasNode("/customers"));

        final JcrResult verifiedCustomers = jcr.executeStoredQuery(VerifiedCustomers.class);
        assertEquals(1, verifiedCustomers.nodes().size());
        assertNotNull(verifiedCustomers.rows().next().getNode(CUSTOMER));
    }

    @Test
    public void queryJoined() throws RepositoryException {
        assumeTrue(jcr.hasNode("/customers"));

        final JcrResult invoicesForSubmission = jcr.executeStoredQuery(CreatedInvoices.class);
        // 2 invoices:
        assertEquals(2, invoicesForSubmission.rows().size());
        // 1 customer:
        assertEquals(1, invoicesForSubmission.rows().stream()
            .map((JcrFunction<JcrRow, String>) row -> row.getNode(CUSTOMER).getName()).distinct().count());
    }

    @Test
    public void queryJoinedWithBindVariable() throws RepositoryException {
        assumeTrue(jcr.hasNode("/customers"));

        final JcrResult createdInvoices = jcr.executeStoredQuery(VerifiedCustomerInvoices.class,
            QueryParameter.of(VerifiedCustomerInvoices.INVOICE_STATUS, jcr.createValue(InvoiceStatus.CREATED)));

        // 2 invoices:
        assertEquals(2, createdInvoices.rows().size());

        // unique names:
        assertThat(createdInvoices.rows().stream()
            .map((JcrFunction<JcrRow, String>) row -> row.getNode(INVOICE).getName()).distinct().count()).isEqualTo(2);

        // 1 customer:
        assertThat(createdInvoices.rows().stream()
            .map((JcrFunction<JcrRow, String>) row -> row.getNode(CUSTOMER).getName()).distinct().count()).isEqualTo(1);
    }

    @Test
    public void removeQueryNodes() throws RepositoryException {
        assumeTrue(jcr.hasNode("/customers"));

        final JcrResult createdInvoices = jcr.executeStoredQuery(VerifiedCustomerInvoices.class,
            QueryParameter.of(VerifiedCustomerInvoices.INVOICE_STATUS, jcr.createValue(InvoiceStatus.CREATED)));

        // 2 invoices:
        assertEquals(2, createdInvoices.rows().size());

        // 1 customer:
        assertThat(createdInvoices.rows().stream()
            .map((JcrFunction<JcrRow, String>) row -> row.getNode(CUSTOMER).getName()).distinct().count()).isEqualTo(1);

        // remove unique customers; distinct Stream won't work because of the deferred nature of Streams
        createdInvoices.rows().stream().map((JcrFunction<JcrRow, String>) row -> row.getNode(CUSTOMER).getPath())
            .distinct().collect(Collectors.toSet()).forEach((JcrConsumer<String>) jcr.session::removeItem);

        jcr.session.save();

        assertTrue(jcr
            .executeStoredQuery(VerifiedCustomerInvoices.class,
                QueryParameter.of(VerifiedCustomerInvoices.INVOICE_STATUS, jcr.createValue(InvoiceStatus.CREATED)))
            .rows().stream().noneMatch((Predicate<JcrRow>) row -> {
                try {
                    row.getNode(CUSTOMER).getPath();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
    }

    @ComponentScan(useDefaultFilters = false)
    @SpringBootApplication
    public static class Application {

        @Bean
        public JcrConsumer<Jcr> repositorySetup() {
            return jcr -> {
                jcr.session.getWorkspace().getNamespaceRegistry().registerNamespace(TestJcrStructure.PREFIX,
                    TestJcrStructure.NS);

                jcr.getOrRegisterNodeType(INVOICE, invoice -> {
                    jcr.addTo(invoice, STATUS);
                    jcr.addTo(invoice, ORDER_DATE);
                });

                jcr.getOrRegisterNodeType(CUSTOMER, customer -> {
                    jcr.addTo(customer, VERIFIED);
                    jcr.addTo(customer, NAME);
                    jcr.addTo(customer, INVOICE);
                });
            };
        }

        @Bean
        public QueryBuilder.Strong<VerifiedCustomers> verifiedCustomers() {
            return new QueryBuilder.Strong<VerifiedCustomers>() {

                @Override
                protected QueryObjectModel buildQuery() throws RepositoryException {
                    return createQuery(selector(CUSTOMER)).constraint(isTrue(propertyValue(VERIFIED.of(CUSTOMER))))
                        .get();
                }
            };
        }

        @Bean
        public QueryBuilder.Strong<CreatedInvoices> createdInvoices(Session session) {
            return new QueryBuilder.Strong<CreatedInvoices>() {

                @Override
                protected QueryObjectModel buildQuery() throws RepositoryException {
                    return createQuery(join(selector(CUSTOMER), selector(INVOICE), JCR_JOIN_TYPE_INNER,
                        childNodeJoinCondition(INVOICE, CUSTOMER)))
                            .constraint(comparison(propertyValue(STATUS.of(INVOICE)), JCR_OPERATOR_EQUAL_TO,
                                literal(Jcr.enumValue(InvoiceStatus.CREATED))))
                            .orderings(ascending(nodeName(CUSTOMER)), ascending(propertyValue(ORDER_DATE.of(INVOICE))))
                            .get();
                }
            };
        }

        @Bean
        public QueryBuilder.Strong<VerifiedCustomerInvoices> verifiedCustomerInvoices() {
            return new QueryBuilder.Strong<VerifiedCustomerInvoices>() {

                @Override
                protected QueryObjectModel buildQuery() throws RepositoryException {
                    return createQuery(join(selector(CUSTOMER), selector(INVOICE), JCR_JOIN_TYPE_INNER,
                        childNodeJoinCondition(INVOICE, CUSTOMER))).constraint(() -> {
                            final Constraint customerVerified = isTrue(propertyValue(VERIFIED.of(CUSTOMER)));
                            final Comparison status = comparison(propertyValue(STATUS.of(INVOICE)),
                                JCR_OPERATOR_EQUAL_TO, bindVariable(VerifiedCustomerInvoices.INVOICE_STATUS));
                            return and(customerVerified, status);
                        }).orderings(ascending(nodeName(CUSTOMER)), ascending(propertyValue(ORDER_DATE.of(INVOICE))))
                            .get();
                }
            };
        }

        @Bean
        public QueryBuilder.Strong<InvoicesByCustomer> invoicesByCustomer() {
            return new QueryBuilder.Strong<InvoicesByCustomer>() {

                @Override
                protected QueryObjectModel buildQuery() throws RepositoryException {
                    return createQuery(join(selector(CUSTOMER), selector(INVOICE), JCR_JOIN_TYPE_INNER,
                        childNodeJoinCondition(INVOICE, CUSTOMER)))
                            .constraint(comparison(propertyValue(NAME.of(CUSTOMER)), JCR_OPERATOR_EQUAL_TO,
                                bindVariable(InvoicesByCustomer.CUSTOMER_NAME)))
                            .orderings(ascending(nodeName(CUSTOMER)), ascending(propertyValue(ORDER_DATE.of(INVOICE))))
                            .get();
                }
            };
        }

        @Bean
        public QueryBuilder.Strong<CustomerWithInvoiceBefore> customerWithInvoiceBefore() {
            return new QueryBuilder.Strong<CustomerWithInvoiceBefore>() {

                @Override
                protected QueryObjectModel buildQuery() throws RepositoryException {
                    return createQuery(join(selector(CUSTOMER), selector(INVOICE), JCR_JOIN_TYPE_INNER,
                        childNodeJoinCondition(INVOICE, CUSTOMER)))
                            .constraint(comparison(propertyValue(ORDER_DATE.of(INVOICE)), JCR_OPERATOR_LESS_THAN,
                                bindVariable(CustomerWithInvoiceBefore.ORDER_DATE)))
                            .orderings(ascending(nodeName(CUSTOMER)), ascending(propertyValue(ORDER_DATE.of(INVOICE))))
                            .get();
                }
            };
        }
    }
}
