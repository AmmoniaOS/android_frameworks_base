#!/bin/bash
make
./generateData Mps -c one-phoneloc.txt
cp one-phoneloc.dat $dirname $(dirname $(dirname $(dirname $(dirname $(pwd)))))/vendor/one/prebuilt/common/media/one-phoneloc.dat
make clean
rm one-phoneloc.dat
