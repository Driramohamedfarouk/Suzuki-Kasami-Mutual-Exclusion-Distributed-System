public class Site2 extends AbstractSite {
    Site2(){
        super();
        setId(1);
        frame.setTitle("Site 2");
        frame.setLocation(400,50);
        updateRN();
        set_up(1);
    }


    public static void main(String[] args) {
        new Site2();
    }
}