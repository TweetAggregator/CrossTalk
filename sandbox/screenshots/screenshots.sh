#!/bin/bash
 
# Thanks to http://www.maketecheasier.com/take-screenshots-in-ubuntu-at-regular-interval/
# Take screenshot at interval of 2 minutes, name them, and put them in the good folder.
# NB: scrot must be installed
i=0;
while(true)
do
   i=$(( $i + 1 ))
   scrot -d 60 "$i.jpg";
done