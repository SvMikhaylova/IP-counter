import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import service.IpCounterService;

public class IpCounterServiceTest {

    @Test
    public void testIpCounter() {
        var count = new IpCounterService().countDistinctIpAddressesOptimized("./src/test/resources/test_ip", 1);
        Assertions.assertEquals(21, count);
    }
}
