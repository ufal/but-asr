# but-asr

## To run
```
docker-compose up
```

## Test asr
```
curl -L -F 'file=@/home/okosarko/lindat_sync_dir/but-asr_en/client/example.wav' http://localhost:9080/asr_lid-client/asr/en
```

## Test lid
```
curl -L -F 'file=@/home/okosarko/lindat_sync_dir/but-asr_en/client/example.wav' http://localhost:9080/asr_lid-client/lid
```

## Test balancing
```
# watch asr (or lid) containers output (docker logs...)
for i in `seq 1 2`; do curl -L -F 'file=@/home/okosarko/lindat_sync_dir/but-asr_en/client/example.wav' http://localhost:9080/asr_lid-client/asr/en & done
```
