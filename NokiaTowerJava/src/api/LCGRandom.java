package api;

public class LCGRandom {

    public int Seed;

    public LCGRandom(int seed)
    {
        this.Seed = seed;
    }

    public int NextInt()
    {
        this.Seed = (1103515245 * this.Seed + 12345) & Integer.MAX_VALUE;
        return this.Seed;
    }

    public int NextInt(int n)
    {
        int divisor = Integer.MAX_VALUE / n;
        int retval;
        do
        {
            retval = NextInt() / divisor;
        } while (retval >= n);
        return retval;
    }

    public double NextDouble()
    {
        return (double)this.NextInt() / Integer.MAX_VALUE;
    }

    private static LCGRandom random = new LCGRandom(0);

    public static int Rnd(int n)
    {
        return random.NextInt(n);
    }

    public static void RndIni(int seed)
    {
        random.Seed = seed;
    }

    public static double RndFloat()
    {
        return random.NextDouble();
    }
    
}
