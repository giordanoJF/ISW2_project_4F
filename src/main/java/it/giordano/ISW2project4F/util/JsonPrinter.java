package it.giordano.ISW2project4F.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.json.JSONObject;

import java.util.Map;

public class JsonPrinter {

    public static void printPrettyJson(JSONObject jsonObject) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

            // Conversion to Map
            Map<String, Object> map = jsonObject.toMap();

            String prettyJson = writer.writeValueAsString(map);
            System.out.println(prettyJson);
        } catch (Exception e) {
            System.err.println("Error printing JSON with Jackson: " + e.getMessage());
        }
    }
}

