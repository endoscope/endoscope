#!/bin/bash

#workaround for GPG error: 'gpg: signing failed: Inappropriate ioctl for device'
export GPG_TTY=$(tty)

mvn release:perform
