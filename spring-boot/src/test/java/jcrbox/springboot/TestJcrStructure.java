package jcrbox.springboot;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;

import jcrbox.JcrNamespace;
import jcrbox.JcrNode;
import jcrbox.JcrProperty;
import jcrbox.JcrProperty.DefaultValue;
import jcrbox.query.JcrQuery;
import jcrbox.query.JcrQuery.PathRoot;

/**
 * Test structure describing a JCR layout.
 */
public class TestJcrStructure {

    /**
     * JCR namespace URI.
     */
    public static final String NS = "http://www.github.com/mbenson/jcrbox/test";

    /**
     * JCR namespace prefix.
     */
    public static final String PREFIX = "test";

    /**
     * Enum describing custom JCR nodes.
     */
    //@formatter:off
    @JcrNamespace(NS)
    public enum Nodes implements JcrNode<Nodes> {
        @NodeDefinition(supertypes = NodeType.NT_RESOURCE)
        INVOICE,

        CUSTOMER;
    }

    /**
     * Enum describing possible states of an invoice.
     */
    public enum InvoiceStatus {
        @DefaultValue
        CREATED,

        STAGED,

        SUBMITTED,

        COMPLETED;
    }

    /**
     * Enum describing custom JCR properties.
     */
    @JcrNamespace(NS)
    public enum Properties implements JcrProperty<Properties> {

        @PropertyDefinition(constrainAsEnum = InvoiceStatus.class, autoCreated = true)
        STATUS,

        @PropertyDefinition(PropertyType.DATE)
        ORDER_DATE,

        @PropertyDefinition(value = PropertyType.STRING, valueConstraints = "[A-Za-z\\. ]+")
        NAME,

        @PropertyDefinition(PropertyType.BOOLEAN)
        VERIFIED;
    }
    //@formatter:on

    /**
     * Structural container for JCR queries.
     */
    @PathRoot
    @JcrNamespace(NS)
    public static class Queries {

        /**
         * Query literal for verified customers.
         */
        public static class VerifiedCustomers implements JcrQuery {
        }

        /**
         * Query literal for created invoices.
         */
        public static class CreatedInvoices implements JcrQuery {
        }

        /**
         * Query literal for invoices belonging to verified customers.
         */
        public static class VerifiedCustomerInvoices implements JcrQuery {

            /**
             * Query parameter name for desired invoice status.
             */
            public static final String INVOICE_STATUS = "invoiceStatus";
        }

        /**
         * Query literal for invoices belonging to a given customer.
         */
        public static class InvoicesByCustomer implements JcrQuery {

            /**
             * Query parameter name for desired customer name.
             */
            public static final String CUSTOMER_NAME = "customerName";
        }

        /**
         * Query literal for customer with invoice before a given date.
         */
        public static class CustomerWithInvoiceBefore implements JcrQuery {

            /**
             * Query parameter name for the order date before which a customer containing invoices will be selected.
             */
            public static final String ORDER_DATE = "orderDate";
        }
    }
}
