#!/bin/bash
 
# Thanks Adrien and http://www.itforeveryone.co.uk/image-to-video.html
# Take all the screenshots and do a video with it, with an interval of 
# NB: require ffmpeg to be installed

ffmpeg -r 4 -i %d.jpg concat.mp4