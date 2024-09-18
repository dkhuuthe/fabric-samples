ssh fl-server "cd temp/CODE/FLApp; ./gradlew run;" &
ssh ml-predict "cd temp/CODE/FLApp; ./gradlew run;" &
wait
echo "finished!"
