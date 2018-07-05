#!/bin/bash
projectname=$1
baseurl="https://issues.apache.org/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+$projectname+ORDER+BY+updated+DESC"
for i in $(seq 0 1000 10000)
  do
      curl --max-time 9000 $baseurl"&tempMax=1000&pager/start="$i > $projectname$i".xml"
 done