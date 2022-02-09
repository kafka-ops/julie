#!/bin/sh


# Maven build the code to generate the julie-ops.jar or download the latest version from 
# https://github.com/kafka-ops/julie/releases


echo "Starting JulieOps!!!"
echo 
 

if [ -f "$./target/julie-ops.jar" ]; then
    echo "julie-ops.jar exists in the ./target folder will execute from there."
    java -jar ./target/julie-ops.jar $@

elif [ -f "julie-ops.jar" ]; then
    echo "julie-ops.jar exists in the current folder will execute from current location."
    java -jar julie-ops.jar $@
else 
   echo "julie-ops.jar is not found, julie-ops.jar is required to execute this shell script!"
   echo "You can either build the code and generate the jar or download it from the following url : "
   echo "https://github.com/kafka-ops/julie/releases"
   echo "code will now exit"
fi




