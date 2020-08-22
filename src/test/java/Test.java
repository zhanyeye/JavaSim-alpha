import org.javasim.SimulationProcess;
import org.javasim.internal.SimulationProcessQueue;
import org.javasim.streams.ExponentialStream;

/**
 * @description:
 * @author: zhanyeye
 * @create: 2020-08-20 15:02
 **/
public class Test {
    public static void main(String[] args) {
        Dummy A = new Dummy(8);
        SimulationProcessQueue queue = new SimulationProcessQueue();
        queue.insert(A);
    }
}


class Dummy extends SimulationProcess
{
    public Dummy (double mean)
    {
        InterArrivalTime = new ExponentialStream(mean);
    }

    public void run ()
    {
        try
        {
            hold(InterArrivalTime.getNumber());
        }
        catch (final Exception ex)
        {
        }
    }

    private ExponentialStream InterArrivalTime;
}
