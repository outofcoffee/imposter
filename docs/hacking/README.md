Test with Gradle:

    docker run -it --rm -u gradle -v "$PWD":/home/gradle/project -w /home/gradle/project amd64/gradle:5.6-jdk8 gradle clean test
