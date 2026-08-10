package com.bank.transfer_service;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "Transfer Service API",
                version = "v1.0",
                description = "API for managing transfers in the banking system",
                contact = @Contact(
                        name = "Manjunath K H",
                        email = "khmanjunatha405@gmail.com"
                )
        ),
        externalDocs = @ExternalDocumentation(
                description = "Transfer Service Documentation",
                url = "https://example.com/docs/transfer-service"
        )
)
@SpringBootApplication
public class TransferServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(TransferServiceApplication.class, args);
    }

}
