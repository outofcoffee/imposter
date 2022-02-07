Import WSDL:

    wsimport -s src/main/java \
        -keep -Xnocompile -extension -encoding UTF-8 \
        -wsdllocation /petservice/service.wsdl \
        src/main/wsdl/service.wsdl
