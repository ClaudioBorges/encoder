@echo off
set inputFile="../_default_files/EncoderServer/EncoderServer.properties"

@echo on
java -cp dist\EncoderServer.jar encoderserver.EncoderClient %inputFile%

@echo off
pause