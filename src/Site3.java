public class Site3 extends AbstractSite {
    Site3(){
        super();
        setId(2);
        frame.setTitle("Site 3");
        frame.setLocation(800,50);
        updateRN();
        set_up(2);
    }


    public static void main(String[] args) {
        new Site3();
    }
}