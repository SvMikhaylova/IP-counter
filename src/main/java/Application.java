import service.IpCounterService;

import java.time.Duration;
import java.time.LocalDateTime;

public class Application {

    public static void main(String[] args) {
        var service = new IpCounterService();
        var filePath = args[0];
        LocalDateTime time1 = LocalDateTime.now();
        var count = service.countDistinctIpAddressedNaive(filePath);
        System.out.println("Count: " + count);
        LocalDateTime time2 = LocalDateTime.now();
        System.out.println("Processing time: " + Duration.between(time1, time2).toMillis());
    }
}
