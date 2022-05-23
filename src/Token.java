import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class Token {
    private static Token instance = null ;
    private int [] LN ;
    private  Queue<Integer> Q ;
    {
        LN = new int[AbstractSite.N];
        for (int i = 0; i <AbstractSite.N; i++) {
            LN[i]= 0 ;
        }
        Q = new LinkedList<>();
    }
    Token(){
    }
    public static Token getInstance() {
        if(instance==null){
            instance = new Token();
        }
        return  instance ;
    }

    public int[] getLN() {
        return LN;
    }

    public Queue getQ() {
        return Q;
    }
}

