public class Request {

    int id ;
    int sequence_number ;

    public Request(int id, int sequence_number) {
        this.id = id;
        this.sequence_number = sequence_number;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSequence_number() {
        return sequence_number;
    }

    public void setSequence_number(int sequence_number) {
        this.sequence_number = sequence_number;
    }
}
