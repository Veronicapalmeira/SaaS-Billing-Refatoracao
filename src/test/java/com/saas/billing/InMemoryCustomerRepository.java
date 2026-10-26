package com.saas.billing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryCustomerRepository implements CustomerRepository {

    record SavedInvoice(String customerId, Money amount, InvoiceStatus status) {
    }

    private final Map<String, Customer> customers = new HashMap<>();
    private final List<SavedInvoice> invoices = new ArrayList<>();

    void add(Customer customer) {
        customers.put(customer.id(), customer);
    }

    List<SavedInvoice> invoices() {
        return invoices;
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    @Override
    public void saveInvoice(String customerId, Money amount, InvoiceStatus status) {
        invoices.add(new SavedInvoice(customerId, amount, status));
    }
}
