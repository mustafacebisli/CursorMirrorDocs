package com.example.demo;

import java.util.Optional;
import java.util.UUID;

import com.example.business.AuditLogger;
import com.example.business.Customer;
import com.example.business.CustomerService;
import com.example.business.EmailVerificationQueue;
import com.example.business.InventoryManager;
import com.example.business.StockItem;

public class Main {

    public static void main(String[] args) {
        AuditLogger audit = (event, detail) ->
                System.out.println("[AUDIT] " + event + " :: " + detail);

        EmailVerificationQueue emailQueue = new EmailVerificationQueue() {
            @Override
            public void enqueueVerification(UUID customerId, String email) {
                System.out.println("[EMAIL] verification requested for " + email
                        + " (customer=" + customerId + ")");
            }

            @Override
            public boolean consumeToken(UUID customerId, String token) {
                return "VALID".equals(token);
            }
        };

        runCustomerFlow(audit, emailQueue);
        System.out.println();
        runInventoryFlow(audit);

        System.out.println();
        System.out.println("Demo finished.");
    }

    private static void runCustomerFlow(AuditLogger audit, EmailVerificationQueue emailQueue) {
        System.out.println("=== Customer flow ===");
        CustomerService customers = new CustomerService(emailQueue, audit);

        Customer alice = customers.create("Alice", "alice@example.com");
        Customer bob = customers.create("Bob", "bob@example.com");

        Customer aliceUpdated = customers.update(
                alice.getId(), "Alice Smith", "alice.smith@example.com");
        System.out.println("Updated -> " + aliceUpdated.getFullName()
                + " <" + aliceUpdated.getEmail() + ">");

        Customer aliceVerified = customers.verifyEmail(alice.getId(), "VALID");
        System.out.println("Verified at -> " + aliceVerified.getEmailVerifiedAt());

        Optional<Customer> bobLookup = customers.findById(bob.getId());
        System.out.println("Lookup bob (active) -> "
                + bobLookup.map(Customer::getFullName).orElse("(not found)"));

        customers.softDelete(bob.getId());
        System.out.println("Lookup bob (after soft delete) -> "
                + customers.findById(bob.getId())
                        .map(Customer::getFullName).orElse("(not found)"));

        try {
            customers.create("Alice2", "alice.smith@example.com");
        } catch (CustomerService.DuplicateEmailException e) {
            System.out.println("Expected duplicate guard -> " + e.getMessage());
        }
    }

    private static void runInventoryFlow(AuditLogger audit) {
        System.out.println("=== Inventory flow ===");
        InventoryManager inventory = new InventoryManager(audit, 5);

        inventory.registerSku("SKU-A", 20);
        inventory.registerSku("SKU-B", 8);

        inventory.reserve("SKU-A", 3);
        inventory.reserve("SKU-B", 6);

        inventory.commit("SKU-A", 3);
        inventory.release("SKU-B", 6);

        inventory.restock("SKU-B", 10);

        StockItem a = inventory.get("SKU-A").orElseThrow();
        StockItem b = inventory.get("SKU-B").orElseThrow();
        System.out.println("SKU-A -> onHand=" + a.getOnHand()
                + " reserved=" + a.getReserved());
        System.out.println("SKU-B -> onHand=" + b.getOnHand()
                + " reserved=" + b.getReserved());

        try {
            inventory.reserve("SKU-A", 999);
        } catch (InventoryManager.OutOfStockException e) {
            System.out.println("Expected stock guard -> " + e.getMessage());
        }
    }
}
