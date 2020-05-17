#!/usr/bin/env bash

check_first_pipe_exit_code() {
  local first_pipe_exit_code=${PIPESTATUS[0]};
  if test $first_pipe_exit_code -ne 0; then
    exit $first_pipe_exit_code;
  fi
}
ACTION=$1;
ACTION2=$2;
ACTION3=$3;
ACTION4=$4;

errordb(){
 cat itemcenter2.log |egrep 'galicdb_[0-9]+' -o|sort |uniq -c
}


jstat(){
    now=`date "+%Y-%m-%d-%H:%M:%S"`
    pid=`ps aux | fgrep "Dproject.name=" | fgrep -v grep | awk '{print $2}'`
    /opt/java/bin/jstat -gcutil $pid 1000
}

jstack(){
    now=`date "+%Y-%m-%d-%H:%M:%S"`
    pid=`ps aux | fgrep "Dproject.name=" | fgrep -v grep | awk '{print $2}'`
    /opt/java/bin/jstack $pid >jstack-$now.log
    green "jstack -> jstack-$now.log"
}
gc(){
    now=`date "+%Y-%m-%d-%H:%M:%S"`
    pid=`ps aux | fgrep "Dproject.name=" | fgrep -v grep | awk '{print $2}'`
    /opt/java/bin/jcmd $pid GC.run
}

call(){
     curl -s "http://localhost:7001/"$ACTION2
}

restart()
{
    echo "see log at stable.log"
    sh /home/admin/start.sh
}
stop()
{
     echo "see log at stable.log"
     sh /home/admin/stop.sh
}
online(){
     result= curl -s "http://localhost:12201/hsf/online?k=hsf"
     echo $result
}
offline(){
  result= curl -s "http://localhost:12201/hsf/offline?k=hsf"
  echo $result
}

usage() {
    red "===========can not find command==========="
    yellow "Usage: $PROG_NAME {}"
    exit 2 # bad usage
}
hsfCommond(){
   if [ "$ACTION2"x == "offline"x ]; then
       result= curl -s 'http://localhost:12201/hsf/'$ACTION2'?k=hsf'
        echo  $result
        exit 0
   fi
   if [ "$ACTION2"x == "online"x ]; then
        result= curl -s 'http://localhost:12201/hsf/'$ACTION2'?k=hsf'
        echo  $result
        exit 0
   fi

   result= curl -s 'http://localhost:12201/hsf/'$ACTION2
   echo  $result
}

ossinstall(){
    osscmd config --host=oss-cn-hangzhou-zmf.aliyuncs.com --id=LTAIS3zFPhCbkQAp --key=x0x8QNxYv07h1Q2JFrelGzEf3sw4PZ
}
ossput(){
   osscmd put $ACTION2 oss://43018
}
ossget(){
    osscmd get oss://43018/$ACTION2 $ACTION2
}
node(){
   while true
    do
       curl -s "http://localhost:8719/cnode?id="$ACTION2
       sleep 1
    done
}

arthas(){
     pid=`ps aux | fgrep "Dproject.name=" | fgrep -v grep | awk '{print $2}'`
   if [[ ! -x "as.sh" ]]; then
    green "wiki:http://gitlab.alibaba-inc.com/middleware-container/arthas/wikis/home"
	curl -L http://start.alibaba-inc.com/install.sh | sh
   fi
   sh as.sh $pid

}

main() {
    now=`date "+%Y-%m-%d %H:%M:%S"`
    echo "$now"
    welcome
    case "$ACTION" in
        restart)
            restart
        ;;
         stop)
            stop
        ;;
         jstack)
            jstack
        ;;
         sv)
            sv
        ;;
         hsf)
            hsfCommond
        ;;
        offline)
            offline
        ;;
        jstat)
            jstat
        ;;
         online)
            online
        ;;
          ss)
            ss
        ;;
          sg)
            sg
        ;;
          errordb)
            errordb
        ;;
         ossinstall)
            ossinstall
        ;;
         ossput)
            ossput
        ;;
          gc)
            gc
        ;;
          tree)
            tree
        ;;
          calljsp)
            call
        ;;
           node)
            node
        ;;
            arthas)
            arthas
        ;;
          ossget)
            ossget
        ;;

        *)
            usage
        ;;
    esac
}








## blue to echo
function blue(){
    echo -e "\033[35m[ $1 ]\033[0m"
}


## green to echo
function green(){
    echo -e "\033[32m[ $1 ]\033[0m"
}

## Error to warning with blink
function bred(){
    echo -e "\033[31m\033[01m\033[05m[ $1 ]\033[0m"
}

## Error to warning with blink
function byellow(){
    echo -e "\033[33m\033[01m\033[05m[ $1 ]\033[0m"
}


## Error
function red(){
    echo -e "\033[31m\033[01m[ $1 ]\033[0m"
}

## warning
function yellow(){
    echo -e "\033[33m\033[01m[ $1 ]\033[0m"
}

welcome(){
    blue  "stable.sh powered by zhaodong.xzd@alibaba-inc.com"
    echo ""
}
main | tee -a /home/admin/itemcenter2/logs/stable.log  ;
