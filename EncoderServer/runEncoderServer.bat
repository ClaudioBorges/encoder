@echo off
set inputFile="../_default_files/EncoderServer/EncoderServer.properties"

@echo on
java -jar dist\EncoderServer.jar %inputFile%

@echo off
pause