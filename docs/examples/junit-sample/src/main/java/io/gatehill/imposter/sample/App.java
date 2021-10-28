package io.gatehill.imposter.sample;

import java.net.URI;

/**
 * Retrieves a list of pets from an API.
 */
public class App {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("You must pass the base URL as an argument to this program.");
            System.exit(1);
        }

        final String baseUrl = args[0];
        final PetService petService = new PetService(URI.create(baseUrl).toURL());

        final Pet[] pets = petService.listPets();
        for (Pet pet : pets) {
            System.out.printf("Pet %d name: %s\n", pet.getId(), pet.getName());
        }
    }
}
