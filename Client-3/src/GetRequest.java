public class GetRequest extends Request{

    public GetRequest(String url, String option) {
        super(url, option);
        if (option.contains("-h ")) {
            if (option.contains("Host")) {
                host = option.substring(option.indexOf(":", option.indexOf("Host")) + 1, option.indexOf(" ", option.indexOf("Host")));
            }

            if (option.contains("User-Agent")) {
                userAgent = option.substring(option.indexOf(":", option.indexOf("User-Agent")) + 1, option.indexOf(" ", option.indexOf("User-Agent")));
            }
        }
    }
}
