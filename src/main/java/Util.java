import me.sargunvohra.lib.pokekotlin.client.PokeApi;
import me.sargunvohra.lib.pokekotlin.client.PokeApiClient;
import me.sargunvohra.lib.pokekotlin.model.NamedApiResource;
import me.sargunvohra.lib.pokekotlin.model.NamedApiResourceList;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

public class Util {
    public static final HashMap<Integer, String> ID_MAP = new HashMap<>();
    public static final HashMap<String, Integer> NAME_MAP = new HashMap<>();

    static {
        try {
            Scanner mapIn = new Scanner(new FileInputStream(new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("map.txt")).toURI())));
            int id = 1;
            while (mapIn.hasNext()) {
                String name = mapIn.nextLine();
                if(name.isBlank()) break;
                ID_MAP.put(id, name);
                NAME_MAP.put(name, id);
                id++;
            }
        } catch (FileNotFoundException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static int maxCount() {
        return ID_MAP.size();
    }

    private static void loadMaps() {
        try {
            Scanner mapIn = new Scanner(new FileInputStream(new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("map.txt")).toURI())));
            int id = 1;
            while (mapIn.hasNext()) {
                String name = mapIn.nextLine();
                if(name.isBlank()) break;
                ID_MAP.put(id, name);
                NAME_MAP.put(name, id);
                id++;
            }
        } catch (FileNotFoundException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void genMaps() throws IOException, URISyntaxException {
        int offset = 0;
        final int count = 10;
        boolean finished = false;

//        HashMap<Integer, String> map = new HashMap<>();

        PokeApi pokeApi = new PokeApiClient();
        OutputStreamWriter mapOut = new OutputStreamWriter(new FileOutputStream(new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("map.txt")).toURI())));
        while(!finished) {
            NamedApiResourceList list = pokeApi.getPokemonList(offset, count);
            List<NamedApiResource> resources = list.getResults();
            offset += count;
            System.out.println(offset + " pokemon completed.");
            for (NamedApiResource resource : resources) {
                if(resource.getId() > 10000) {
                    finished = true;
                    break;
                }
                mapOut.append(resource.getName()).append('\n');
            }
        }

//        System.out.println(map.size());
//        for(Integer key : map.keySet()){
//            mapOut.append(String.valueOf(key)).append(" ").append(map.get(key)).append('\n');
//        }
        mapOut.close();
    }
}
