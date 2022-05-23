import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;


//TODO : consider state DP and set up
//TODO : merge the receive Token and receive request

public abstract class AbstractSite extends JFrame {

    public JFrame frame = new JFrame();

    JLabel label ,label2 ,label3 ,label4 ;
    JButton buttonCS;
    JButton buttonReleaseCS;

    public final static  int N = 3 ;

    private int  id  ;
    private boolean doHaveTheToken = false ;
    private Status status = Status.NOT_REQUESTING ;
    //RN[j] designates the
    //sequence number of the latest request received from process j
    private static int[] RN = new int[N]  ;
    private static final String[] QUEUE_NAMES = new String[N];
    private static final String[] TOKEN_TO = new String[N];

    Token token = null ;
    Gson gson = null ;

    public AbstractSite(){
        buttonCS = new JButton("Enter critical section");
        buttonReleaseCS = new JButton("Release critical section ");
        frame.setLayout(new GridLayout(6,1));
        label = new JLabel();
        label2 = new JLabel();
        label3 = new JLabel();
        label4 = new JLabel();
        label.setText(status.name());
        updateRN();
        updateToken();

        buttonReleaseCS.setVisible(false);

        frame.add(label);
        frame.add(label3);
        frame.add(buttonCS);
        frame.add(buttonReleaseCS);
        frame.add(label2);
        frame.add(label4);

        frame.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLocation(50, 50);
        frame.setVisible(true);

        buttonCS.addActionListener(e -> request_CS());
        buttonReleaseCS.addActionListener(e -> release_CS());

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        this.gson = builder.create();

    }


    public void set_up(int id ){
        for(int i=0 ; i<N ; i++){
            RN[i] = 0 ;
            QUEUE_NAMES[i] = "from "+id+" to "+i;
            TOKEN_TO[i] = "token to "+i ;
            if(i!=id){
                try {
                    receive_Request_For_CS(i);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            receive_Token();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public synchronized void request_CS(){
        setStatus(Status.REQUESTING);
        if(token==null){
            RN[id] ++ ;
            updateRN();
            //TODO : transform this loop into a broadcast method in BrokerUtils
            for (int i = 0; i < N; i++) {
                if(i!=id){
                    //emit Message to all sites with
                    // REQUEST(id,RN[id])
                    BrokerUtils.emitMessage(id+":"+RN[id],QUEUE_NAMES[i]);
                    System.out.println("sent request to "+i);
                }
            }
        }else {
            setStatus(Status.CRITICAL_SECTION);
            buttonReleaseCS.setVisible(true);
            //EXECUTE CS
        }

    }

    public synchronized void release_CS(){

        assert  token!=null ;
        // set the last executed CS to the last Request
        token.getLN()[id] = RN[id];
        // check if other sites are requesting for the Token
        for(int site=0 ; site<N;site++) {
            /*
            if there is a site requesting for the Token and is not already
            on the Queue add it to the end of the queue
             */
            if ((site != id) &&
                    !(token.getQ().contains(site)) &&
                    (RN[site] > token.getLN()[site])) {
                token.getQ().add(site);
            }
        }
        /*
            if the queue is not empty send the Token to the head of the queue
             */
        if(!token.getQ().isEmpty()){
            int new_site = (int) token.getQ().remove();
            /*
            Transform the
            * */
            //emit token message to the new site
            String jsonToken = gson.toJson(this.token) ;
            BrokerUtils.emitMessage(jsonToken,TOKEN_TO[new_site]);
            setToken(null);
        }
        setStatus(Status.NOT_REQUESTING);
        buttonReleaseCS.setVisible(false);
    }

    public synchronized void receive_Request_For_CS(int site ) throws IOException, TimeoutException {
        /*
        if i have the token and i dont need the critical section now
        and the request is not outdated
        then send the token to the site requesting it
        otherwise update my request list
         */
        String queue_name = "from "+site+" to "+id;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queue_name, false, false, false, null);
        System.out.println(" [*] Waiting for requests for CS from "+site+" to "+id);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
            this.label2.setText("Received '" + message + "'");
            String[] arr = message.split(":");
            int sequence_number = Integer.parseInt(arr[1]);
            int id = Integer.parseInt(arr[0]);
            RN[id]= Math.max(sequence_number, RN[id]);
            updateRN();
            if(token!=null && status!=Status.CRITICAL_SECTION &&
                    RN[id] > token.getLN()[id] ){
                //setDoHaveTheToken(false);
                // serialize the token
                // send the token to site
                String jsonToken = gson.toJson(token);
                BrokerUtils.emitMessage(jsonToken,TOKEN_TO[site]);
                setToken(null);
            }
            /*if(sequence_number > RN[site]){
                System.out.println("OK");
            }else{
                System.out.println("the request is outdated\n ");
                //System.exit(0);
                // the request is outdated
            }*/
        };
        channel.basicConsume(queue_name, true, deliverCallback, consumerTag -> { });
    }

    public synchronized void receive_Token()  throws IOException, TimeoutException{

        assert this.status==Status.REQUESTING ;

        String queue_name = TOKEN_TO[id];
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queue_name, false, false, false, null);
        System.out.println(" [*] Waiting for the Token ");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            //System.out.println(" [x] Received '" + message + "'");
            // TODO  : deserialize the token
            Token received_token = gson.fromJson(message, Token.class);
            this.setToken(received_token);
            // LN[id]<-RN[id]
            /*
            * For each process k, process i retains process k’s name in its local queue Q only if
            * 1 + last[k] = req[k] (this establishes that the request from process k is a recent one).
            * */

            setStatus(Status.CRITICAL_SECTION);
            buttonReleaseCS.setVisible(true);
        };
        channel.basicConsume(queue_name, true, deliverCallback, consumerTag -> { });
    }

    public boolean isDoHaveTheToken() {
        return doHaveTheToken;
    }

    public void setDoHaveTheToken(boolean doHaveTheToken) {
        this.doHaveTheToken = doHaveTheToken;

    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        label.setText(getStatus().name());
    }

    public int[] getRN() {
        return RN;
    }

    public void setRN(int[] RN) {
        this.RN = RN;
    }

    public void updateRN(){
        label3.setText("RN"+(getId()+1)+" : "+Arrays.toString(RN));
    }

    public static int getN() {
        return N;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private void updateToken() {
        if(token!=null){
            label4.setText("Token \n"+"LN: "+Arrays.toString(token.getLN())+
                    "  Q :"+Arrays.toString(token.getQ().toArray()));
        }else{
            label4.setText("do not have the token ");
        }
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
        updateToken();
    }
}
