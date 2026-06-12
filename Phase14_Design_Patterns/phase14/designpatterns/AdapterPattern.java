package phase14.designpatterns;

// Adapter Pattern: Target interface, Adaptee, Adapter (class vs object adapter)

// =============================================
// Existing / Legacy system (Adaptee)
// =============================================

// Legacy XML weather service (cannot be changed)
class LegacyWeatherService {
    public String getWeatherXML(String cityCode) {
        // In real life, this returns XML like:
        // <weather><city>NYC</city><temp>22.5</temp><condition>Sunny</condition></weather>
        return String.format(
                "<weather><city>%s</city><temp>%.1f</temp><condition>%s</condition></weather>",
                cityCode, 22.5 + Math.random() * 10, "Sunny");
    }
}

// Legacy JSON library that only accepts JSON
class JSONParser {
    public void parse(String json) {
        System.out.println("  [JSON Parser] Parsed: " + json);
    }
}

// =============================================
// Target interface (what the client expects)
// =============================================

// Modern interface: client expects JSON weather data
interface WeatherService {
    String getWeather(String cityCode);
}

// Modern interface: client expects to work with JSON
interface DataParser {
    void parseData(String data);
}

// =============================================
// Object Adapter (using composition)
// =============================================

// Object Adapter: WeatherService adapter
class WeatherServiceObjectAdapter implements WeatherService {
    private final LegacyWeatherService legacyService;

    public WeatherServiceObjectAdapter(LegacyWeatherService legacyService) {
        this.legacyService = legacyService;
    }

    @Override
    public String getWeather(String cityCode) {
        // Adapt: call legacy XML method, convert result to JSON
        String xmlData = legacyService.getWeatherXML(cityCode);
        String jsonData = convertXMLtoJSON(xmlData);
        System.out.println("  [Object Adapter] Converted XML to JSON");
        return jsonData;
    }

    private String convertXMLtoJSON(String xml) {
        // Simple XML to JSON conversion (demonstration only)
        String city = extractXMLValue(xml, "city");
        String temp = extractXMLValue(xml, "temp");
        String condition = extractXMLValue(xml, "condition");
        return String.format("{\"city\":\"%s\",\"temperature\":%s,\"condition\":\"%s\"}",
                city, temp, condition);
    }

    private String extractXMLValue(String xml, String tag) {
        String openTag = "<" + tag + ">";
        String closeTag = "</" + tag + ">";
        int start = xml.indexOf(openTag) + openTag.length();
        int end = xml.indexOf(closeTag);
        return xml.substring(start, end);
    }
}

// Object Adapter: XML to JSON parser adapter
class XMLToJSONAdapter implements DataParser {
    private final LegacyWeatherService legacyService;

    public XMLToJSONAdapter(LegacyWeatherService legacyService) {
        this.legacyService = legacyService;
    }

    @Override
    public void parseData(String cityCode) {
        // Adapt: get XML, convert to JSON, then parse as JSON
        String xml = legacyService.getWeatherXML(cityCode);
        String json = convertSimpleXMLToJSON(xml);
        System.out.println("  [Object Adapter] Adapted XML to JSON:");
        var jsonParser = new JSONParser();
        jsonParser.parse(json);
    }

    private String convertSimpleXMLToJSON(String xml) {
        return xml
                .replace("<weather>", "{")
                .replace("</weather>", "}")
                .replace("<city>", "\"city\":\"")
                .replace("</city>", "\",")
                .replace("<temp>", "\"temperature\":")
                .replace("</temp>", ",")
                .replace("<condition>", "\"condition\":\"")
                .replace("</condition>", "\"}")
                .replace(",}", "}");
    }
}

// =============================================
// Class Adapter (using inheritance)
// Note: Java single inheritance limits this approach;
// the adapter extends the adaptee and implements the target interface
// =============================================

// Class Adapter: WeatherService adapter (extends legacy, implements target)
class WeatherServiceClassAdapter extends LegacyWeatherService implements WeatherService {

    @Override
    public String getWeather(String cityCode) {
        // Call inherited method
        String xmlData = getWeatherXML(cityCode);
        return convertXMLtoJSON(xmlData);
    }

    private String convertXMLtoJSON(String xml) {
        String city = extract(xml, "city");
        String temp = extract(xml, "temp");
        String condition = extract(xml, "condition");
        return String.format("{\"city\":\"%s\",\"temperature\":%s,\"condition\":\"%s\"}",
                city, temp, condition);
    }

    private String extract(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = xml.indexOf(open) + open.length();
        int end = xml.indexOf(close);
        return xml.substring(start, end);
    }
}

// =============================================
// Another adapter example: Media Player
// =============================================

// Target interface
interface MediaPlayer {
    void play(String audioType, String fileName);
}

// Adaptee 1
class AudioPlayer {
    public void playMp3(String fileName) {
        System.out.println("  [AudioPlayer] Playing MP3: " + fileName);
    }
}

// Adaptee 2 (advanced)
class AdvancedMediaPlayer {
    public void playVlc(String fileName) {
        System.out.println("  [AdvancedMediaPlayer] Playing VLC: " + fileName);
    }

    public void playMp4(String fileName) {
        System.out.println("  [AdvancedMediaPlayer] Playing MP4: " + fileName);
    }

    public void playFlac(String fileName) {
        System.out.println("  [AdvancedMediaPlayer] Playing FLAC: " + fileName);
    }
}

// Object Adapter for Media Player
class MediaAdapter implements MediaPlayer {
    private final AdvancedMediaPlayer advancedPlayer;
    private final AudioPlayer audioPlayer;

    public MediaAdapter() {
        this.advancedPlayer = new AdvancedMediaPlayer();
        this.audioPlayer = new AudioPlayer();
    }

    @Override
    public void play(String audioType, String fileName) {
        switch (audioType.toLowerCase()) {
            case "mp3" -> audioPlayer.playMp3(fileName);
            case "vlc" -> advancedPlayer.playVlc(fileName);
            case "mp4" -> advancedPlayer.playMp4(fileName);
            case "flac" -> advancedPlayer.playFlac(fileName);
            default -> throw new IllegalArgumentException("Format " + audioType + " not supported");
        }
    }
}

// Client class
class MediaPlayerClient implements MediaPlayer {
    private final MediaAdapter adapter;

    public MediaPlayerClient() {
        this.adapter = new MediaAdapter();
    }

    @Override
    public void play(String audioType, String fileName) {
        System.out.println("\n  [Client] Requested: " + audioType + " file: " + fileName);
        adapter.play(audioType, fileName);
    }
}

public class AdapterPattern {
    public static void main(String[] args) {
        System.out.println("=== Adapter Pattern Demo ===\n");

        // 1. Object Adapter (composition)
        System.out.println("1. Object Adapter (composition):");
        var legacyService = new LegacyWeatherService();
        WeatherService objectAdapter = new WeatherServiceObjectAdapter(legacyService);

        String weatherJson = objectAdapter.getWeather("NYC");
        System.out.println("  Result: " + weatherJson);

        // 2. Adapter with different target interface
        System.out.println("\n2. Object Adapter (XML -> JSON conversion):");
        DataParser xmlAdapter = new XMLToJSONAdapter(legacyService);
        xmlAdapter.parseData("LAX");

        // 3. Class Adapter (inheritance)
        System.out.println("\n3. Class Adapter (inheritance):");
        WeatherService classAdapter = new WeatherServiceClassAdapter();
        String weatherJson2 = classAdapter.getWeather("LON");
        System.out.println("  Result: " + weatherJson2);

        // 4. Media Player example (another adapter scenario)
        System.out.println("\n4. Media Player Adapter:");
        var player = new MediaPlayerClient();
        player.play("mp3", "song.mp3");
        player.play("mp4", "video.mp4");
        player.play("vlc", "movie.vlc");
        player.play("flac", "lossless.flac");

        // 5. Comparing direct legacy usage vs adapted usage
        System.out.println("\n5. Direct vs Adapted Usage:");
        System.out.println("  Direct (Legacy): " + legacyService.getWeatherXML("CHI"));
        System.out.println("  Adapted (Modern): " + objectAdapter.getWeather("CHI"));

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Target interface - what the client expects/uses");
        System.out.println("Adaptee - existing class with incompatible interface");
        System.out.println("Object Adapter (composition) - wraps Adaptee, implements Target");
        System.out.println("Class Adapter (inheritance) - extends Adaptee, implements Target");
        System.out.println("Client - uses the Target interface, unaware of the Adaptee");
        System.out.println("Multiple Adaptees - adapter can work with multiple incompatible classes");
    }
}
