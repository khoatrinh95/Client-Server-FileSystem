# Assignment1

A simple HTTP client application

To run:
1. Open the package with your ide
2. Run httpc class
3. In the console, run the command 
   - `httpc get -v 'http://httpbin.org/get?course=networking&assignment=1'`
     - to test -v verbose
   - `httpc post -h Content-Type:application/json --d '{"Assignment": 1}' http://httpbin.org/post`
     - to test -h header
   - `httpc post -h Content-Type:application/json --d '{"Assignment": 1, "Test": 3}' http://httpbin.org/post`
     - to test -d inline data
   - `httpc post -h Content-Type:application/json -f src/bodyData http://httpbin.org/post`
     - to test -f file
   - `httpc post -h Content-Type:application/json --d '{"Assignment": 1, "Test": 3}' -f src/name1/name2 http://httpbin.org/post`
     - to test both -d and -f 
4. run `httpc help [get|post]` for help
    - adding `get` or `post` to the request outputs specific help options for either a get or post request respectibly


# Assignment 2
To run Server:

3. In the console, run the command
    - `httpfs `
        - to run server on port 8080, data path /
    - `httpfs -v -d /data`
        - to run server on port 8080, with debugging message, data path /data
    - `httpfs -v -p 8010 -d /data`
        - to run server on port 8010, with debugging message, data path /data
        
To run Client:

3. In the console, run the command
    - `httpc get -v 'http://localhost:8080/'`
        - to test -v verbose
    - `httpc get -v 'http://localhost:8080/../'`
        - to test 403 FORBIDDEN
    - `httpc get -v 'http://localhost:8080/foo'`
        - to test getting content of file named foo
    - `httpc get -v 'http://localhost:8080/foo123'`
        - to test 404 FILE NOT FOUND
    - `httpc post -h Content-Type:application/json --d '{"Assignment": 1}' http://localhost:8080/bar`
        - to test post content to file named bar

# Assignment 3
Packet type:

- 0: SYN
- 1: SYN-ACK
- 2: ACK
- 3: NAK
- 4: Data