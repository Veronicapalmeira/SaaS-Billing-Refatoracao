package com.saas.billing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcCustomerRepository implements CustomerRepository {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/saas_db";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin123";

    private static final String SQL_FIND_CUSTOMER = "SELECT email, status FROM customers WHERE id = ?";
    private static final String SQL_INSERT_INVOICE =
            "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";

    @Override
    public Optional<Customer> findById(String customerId) {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_CUSTOMER)) {
            stmt.setString(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Customer(customerId, rs.getString("email"), rs.getString("status")));
            }
        } catch (SQLException e) {
            throw new CustomerRepositoryException("Erro ao buscar o cliente: " + customerId, e);
        }
    }

    @Override
    public void saveInvoice(String customerId, Money amount, InvoiceStatus status) {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_INVOICE)) {
            stmt.setString(1, customerId);
            stmt.setDouble(2, amount.amount().doubleValue());
            stmt.setString(3, status.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CustomerRepositoryException("Erro ao registrar a fatura do cliente: " + customerId, e);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
}
