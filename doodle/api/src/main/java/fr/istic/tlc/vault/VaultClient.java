package fr.istic.tlc.vault;

import java.io.IOException;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class VaultClient {

    private static final String VAULT_ADDR = "http://localhost:8200";
    private static final String VAULT_TOKEN = "root";
    private static final String SECRET_PATH = "/v1/secret/data/api";

    public static String getApiKey() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .url(VAULT_ADDR + SECRET_PATH)
            .header("X-Vault-Token", VAULT_TOKEN)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String jsonBody = response.body().string();
                JSONObject json = new JSONObject(jsonBody);
                return json.getJSONObject("data")
                           .getJSONObject("data")
                           .getString("API_KEY");
            } else {
                System.err.println(" Erreur HTTP Vault : " + response.code());
            }
        } catch (IOException e) {
            System.err.println(" Erreur communication Vault : " + e.getMessage());
        }

        return null; // ou lancer une exception si tu préfères
    }

    // Pour test uniquement
    public static void main(String[] args) {
        String apiKey = getApiKey();
        System.out.println("API_KEY récupéré : " + apiKey);
    }
}
