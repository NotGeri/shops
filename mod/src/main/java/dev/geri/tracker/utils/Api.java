package dev.geri.tracker.utils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.joml.Vector3i;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Api {

    private final String BASE_URL = "http://127.0.0.1:8000";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(Math.round(Runtime.getRuntime().availableProcessors() * 0.25f), 1));

    private Data data = null;

    public void init() throws IOException {
        // Do the initial API call
        Request request = new Request.Builder().url(BASE_URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected API response " + response);

            // Parse the response
            this.data = this.gson.fromJson(response.body().string(), Data.class);

            // Double check to make sure we have all the data or default
            if (this.data == null) this.data = new Data(new Vector3i[]{}, new ArrayList<>(), new HashMap<>(), new ArrayList<>());
            if (this.data.spawn == null) this.data.spawn = new Vector3i[]{};
            if (this.data.containers == null) this.data.containers = new HashMap<>();
            if (this.data.shops == null) this.data.shops = new ArrayList<>();
            if (this.data.ignored == null) this.data.ignored = new ArrayList<>();

            // Add a reference to the shop
            for (Container container : this.data.containers.values()) {
                container.shop = this.data.shops.stream().filter(shop1 -> Objects.equals(shop1.id(), container.shopId)).findFirst().orElse(null);
            }
        }
    }

    /**
     * Mark a specific container as ignored and submit it to the API
     * // Todo (notgeri): eventually we should have a call to clean up ones that aren't containers anymore
     */
    public void addIgnored(int x, int y, int z) {
        Vector3i v = new Vector3i(x, y, z);
        this.data.ignored.add(v);
        this.executor.submit(() -> {
            try (Response response = client.newCall(new Request.Builder().url(BASE_URL + "/ignored").header("Content-Type", "application/json").post(RequestBody.create(gson.toJson(v).getBytes())).build()).execute()) {
                System.out.println("ignored " + response.code() + " " + response.body().string());
            } catch (IOException exception) {
                System.out.println("unable to add to ignore"); // Todo (notgeri): swap to logger
                this.data.ignored.remove(v);
            }
        });
    }

    public Container saveContainer(Container container) {
        try (Response response = client.newCall(new Request.Builder().url(BASE_URL + "/containers").header("Content-Type", "application/json").post(RequestBody.create(gson.toJson(container).getBytes())).build()).execute()) {

            Container newData = this.gson.fromJson(response.body().string(), Container.class);
            this.data.containers.put(this.formatId(newData.location), newData);
            return newData;

        } catch (IOException exception) {
            System.out.println("unable to saved"); // Todo (notgeri): swap to logger
            return null;
        }
    }

    public List<Shop> shops() {
        return this.data.shops;
    }

    public List<Vector3i> ignored() {
        return this.data.ignored;
    }

    public Vector3i[] spawn() {
        return this.data.spawn;
    }

    public static class Data {
        private Vector3i[] spawn;
        private ArrayList<Shop> shops;
        private HashMap<String, Container> containers;
        private List<Vector3i> ignored;

        public Data(Vector3i[] spawn, ArrayList<Shop> shops, HashMap<String, Container> containers, List<Vector3i> ignored) {
            this.containers = containers;
            this.shops = shops;
            this.spawn = spawn;
            this.ignored = ignored;
        }

        public Vector3i[] spawn() {
            return spawn;
        }

        public ArrayList<Shop> shops() {
            return shops;
        }

        public HashMap<String, Container> containers() {
            return containers;
        }

        public List<Vector3i> ignored() {
            return ignored;
        }
    }

    public record Shop(
            String id,
            String name,
            List<String> owners,
            Vector3i location
    ) {}

    public enum Per {
        @SerializedName("piece") PIECE,
        @SerializedName("stack") STACK,
        @SerializedName("shulker") SHULKER
    }

    public static final class Container {
        private String id;
        private String shopId;
        private String icon;
        private Vector3i location;
        private int price;
        private Per per;
        private int amount;
        private int stocked;
        private String customName;
        private long lastChecked;

        private transient Shop shop;

        public String id() {
            return id;
        }

        public Container setId(String id) {
            this.id = id;
            return this;
        }

        public String shopId() {
            return shopId;
        }

        public Container setShopId(String shopId) {
            this.shopId = shopId;
            return this;
        }

        public String icon() {
            return icon;
        }

        public Container setIcon(String icon) {
            this.icon = icon;
            return this;
        }

        public Vector3i location() {
            return location;
        }

        public Container setLocation(Vector3i location) {
            this.location = location;
            return this;
        }

        public int price() {
            return price;
        }

        public Container setPrice(int price) {
            this.price = price;
            return this;
        }

        public Per per() {
            return per;
        }

        public Container setPer(Per per) {
            this.per = per;
            return this;
        }

        public int amount() {
            return amount;
        }

        public Container setAmount(int amount) {
            this.amount = amount;
            return this;
        }

        public int stocked() {
            return stocked;
        }

        public Container setStocked(int stocked) {
            this.stocked = stocked;
            return this;
        }

        public String customName() {
            return customName;
        }

        public Container setCustomName(String customName) {
            this.customName = customName;
            return this;
        }

        public long lastChecked() {
            return lastChecked;
        }

        public Container setLastChecked(long lastChecked) {
            this.lastChecked = lastChecked;
            return this;
        }

        public Shop shop() {
            return shop;
        }

        public Container setShop(Shop shop) {
            this.shopId = shop.id;
            this.shop = shop;
            return this;
        }
    }

    private String formatId(Vector3i location) {
        return this.formatId(location.x, location.y, location.z);
    }

    private String formatId(int x, int y, int z) {
        return "%s_%s_%s".formatted(x, y, z);
    }

    public Container getContainer(int x, int y, int z) {
        return this.data.containers.get(this.formatId(x, y, z));
    }

}
