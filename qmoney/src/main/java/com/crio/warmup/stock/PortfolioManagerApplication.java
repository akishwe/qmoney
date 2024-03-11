
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {


  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@d29f28";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplicationTest.mainReadFile";
    String lineNumberFromTestFileInStackTrace = "29";


   return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
       toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
       lineNumberFromTestFileInStackTrace});
 }
  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  //  Task:
  //       - Read the json file provided in the argument[0], The file is available in the classpath.
  //       - Go through all of the trades in the given file,
  //       - Prepare the list of all symbols a portfolio has.
  //       - if "trades.json" has trades like
  //         [{ "symbol": "MSFT"}, { "symbol": "AAPL"}, { "symbol": "GOOGL"}]
  //         Then you should return ["MSFT", "AAPL", "GOOGL"]
  //  Hints:
  //    1. Go through two functions provided - #resolveFileFromResources() and #getObjectMapper
  //       Check if they are of any help to you.
  //    2. Return the list of all symbols in the same order as provided in json.

  //  Note:
  //  1. There can be few unused imports, you will need to fix them to make the build pass.
  //  2. You can use "./gradlew build" to check if your code builds successfully.

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException,RuntimeException {
    List<String> symbols = new ArrayList<>();

    if (args == null || args.length == 0 || args[0].isEmpty()) {
        throw new IllegalArgumentException("Please provide a valid filename as an argument.");
    }

    File file = resolveFileFromResources(args[0]);

    ObjectMapper objectMapper = getObjectMapper();
    JsonNode rootNode = objectMapper.readTree(file);
    
    if (rootNode.isArray()) {
        for (JsonNode tradeNode : rootNode) {
            if (tradeNode.has("symbol")) {
                symbols.add(tradeNode.get("symbol").asText());
            }
        }
    }

    return symbols;
  }
// TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.
  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
  PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    double total_num_years = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422;
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double annualized_returns = Math.pow((1.0 + totalReturns), (1.0 / total_num_years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualized_returns, totalReturns);
  }




  // TODO:
  //  Ensure all tests are passing using below command
  //  ./gradlew test --tests ModuleThreeRefactorTest
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  }
  
  
  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size() - 1).getClose();
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    RestTemplate restTemplate = new RestTemplate();
    String tiingoRestURL = prepareUrl(trade, endDate, token);
    TiingoCandle[] tiingoCandleArray =
        restTemplate.getForObject(tiingoRestURL, TiingoCandle[].class);
    return Arrays.stream(tiingoCandleArray).collect(Collectors.toList());
  }

 public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
     throws IOException, URISyntaxException {
      List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
      List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
      String tiingoApiToken = getToken();
      
      // Geting date from comandline args
      LocalDate localDate = LocalDate.parse(args[1]);
    
      for (PortfolioTrade portfolioTrade : portfolioTrades) {
        List<Candle> candles = fetchCandles(portfolioTrade, localDate, tiingoApiToken);
        AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(localDate, portfolioTrade,
            getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles));
        annualizedReturns.add(annualizedReturn);
      }
      return annualizedReturns.stream()
          .sorted((a1, a2) -> Double.compare(a2.getAnnualizedReturn(), a1.getAnnualizedReturn()))
          .collect(Collectors.toList());
 }


  // TODO: CRIO_TASK_MODULE_REST_API
  //  Find out the closing price of each stock on the end_date and return the list
  //  of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  //    and deserialize the results in List<Candle>

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    String filename = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);

    List<PortfolioTrade> trades = readTradesFromJson(filename);
    Map<String, Double> symbolToClosingPriceMap = new HashMap<>();

    RestTemplate restTemplate = new RestTemplate();
    String tiingoApiToken = getToken();
    for (PortfolioTrade trade : trades) {
        String url = prepareUrl(trade, endDate, tiingoApiToken); 

        TiingoCandle[] candles = restTemplate.getForObject(url, TiingoCandle[].class);

            Double closingPrice = candles[candles.length-1].getClose();
            symbolToClosingPriceMap.put(trade.getSymbol(), closingPrice);
    }

    List<String> sortedStockSymbols = symbolToClosingPriceMap.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

    return sortedStockSymbols;
}

  // TODO:
  //  After refactor, make sure that the tests pass by using these two commands
  //  ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  //  ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
    ObjectMapper objectMapper = getObjectMapper();
    File jsonFile = resolveFileFromResources(filename);
    List<PortfolioTrade> trades = objectMapper.readValue(jsonFile, new TypeReference<List<PortfolioTrade>>() {});

    return trades;
}
  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
  public static String getToken() {
    String apiToken = "9ae82dadf586b40d2408fce2efb93fb8d8d83b23";
    return apiToken;
}
  // TODO:
  //  Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    String symbol = trade.getSymbol();
    String startDate = trade.getPurchaseDate().toString(); 

    return "https://api.tiingo.com/tiingo/daily/" + symbol +
           "/prices?startDate=" + startDate +
           "&endDate=" + endDate.toString() +
           "&token=" + token;
}
private static String readFileAsString(String fileName) throws IOException, URISyntaxException {
  return new String(Files.readAllBytes(resolveFileFromResources(fileName).toPath()), "UTF-8");
}

public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args) throws Exception {
  String file = args[0];
  LocalDate endDate = LocalDate.parse(args[1]);
  String contents = readFileAsString(file);
  ObjectMapper objectMapper = getObjectMapper();
  PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(new RestTemplate());
  List<PortfolioTrade> portfolioTrades = objectMapper.readValue(contents, new TypeReference<List<PortfolioTrade>>() {});
  return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
}







  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());




  }
}

