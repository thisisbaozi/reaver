package io.messaginglabs.reaver.com;

public class RemoteServerConnector extends AbstractServerConnector {

    private final boolean debug;
    private final Transporter transporter;

    public RemoteServerConnector(int count, boolean debug, Transporter transporter) {
        super(count);

        this.debug = debug;
        this.transporter = transporter;
    }

    protected Server newServer(String ip, int port) {
        return new RemoteServer(ip, port, debug, transporter) {
            @Override
            protected void deallocate() {
                closeServer(this);
            }
        };
    }

}
