public class Site1 extends AbstractSite {

    Site1(){
        super();
        setId(0);
        frame.setTitle("Site 1");
        /*
        The token will start at the site 1 , this is arbitrary
        * */
        setToken(Token.getInstance());
        updateRN();
        set_up(0);
    }


    public static void main(String[] args) {
        new Site1();
    }
}
