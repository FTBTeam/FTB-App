urlencode() {
  local string="${1}"
  local strlen=${#string}
  local encoded=""
  local pos c o

  for (( pos=0 ; pos<strlen ; pos++ )); do
     c=${string:${pos}:1}
     case "$c" in
        [-_.~a-zA-Z0-9] ) o="${c}" ;;
        * )               printf -v o '%%%02x' "'$c"
     esac
     encoded+="${o}"
  done
  filename="${encoded}"   #+or echo the result (EASIER)... or both... :p
}

filename=$(ls | grep *.opk | tr -d '\n')
urlencode "$filename"
curl -i -X POST -H 'Content-Type: application/json' -d '{"text": "A new Overwolf OPK is available <'"$CI_PROJECT_URL"'/-/jobs/'"$CI_JOB_ID"'/artifacts/raw/'"$filename"'|here>"}' $SLACK_HOOK
