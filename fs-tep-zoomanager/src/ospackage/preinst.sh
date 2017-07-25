# check that owner group exists
if ! getent group fstep &>/dev/null ; then
  groupadd fstep
fi

# check that user exists
if ! getent passwd fstep &>/dev/null ; then
  useradd --system --gid fstep fstep
fi

# (optional) check that user belongs to group
if ! id -G -n fstep | grep -qF fstep ; then
  usermod -a -G fstep fstep
fi

# Make application binary mutable if it already exists (i.e. this is a package upgrade)
if test -f /var/fs-tep/zoomanager/fs-tep-zoomanager.jar ; then
    chattr -i /var/fs-tep/zoomanager/fs-tep-zoomanager.jar
fi
