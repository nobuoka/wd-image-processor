.\gradlew installDist

docker build --platform=linux -t app .

docker run -it -d --name test1 app

sleep 5
docker logs test1

docker stop -t 15 test1

echo "wait"
docker wait test1
docker logs test1
docker rm test1
