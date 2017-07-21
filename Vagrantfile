# -*- mode: ruby -*-
# vi: set ft=ruby :

# This Vagrantfile may require the following vagrant plugins:
# * vagrant-vbguest (for shared folders in the centos/7 base image)
# * vagrant-puppet-install (for the Puppet provisioner)
#
# They may be installed with "vagrant plugin install <plugin>"

Vagrant.configure('2') do |config|

  config.vm.define 'build', primary: false, autostart: false do |build|
    build.ssh.username = 'fstep'
    build.ssh.password = 'fstep'
    build.vm.synced_folder '.', '/home/fstep/build'
    build.vm.synced_folder `echo $HOME`.chomp + '/.gradle', '/home/fstep/.gradle', create: true

    build.vm.provider 'docker' do |d|
      d.build_dir = './build'
      d.build_args = ['--build-arg=http_proxy', '--build-arg=https_proxy', '--build-arg=no_proxy']
      # Change the internal 'fstep' uid to the current user's uid, and launch sshd
      d.cmd = ['/usr/sbin/sshdBootstrap.sh', `id -u`.chomp, `id -g`.chomp, `stat -c %g /var/run/docker.sock`.chomp, '/usr/sbin/sshd', '-D', '-e']
      d.has_ssh = true
      d.create_args = ['--group-add='+`stat -c %g /var/run/docker.sock`.chomp]
      d.volumes = ['/var/run/docker.sock:/var/run/docker.sock:rw']
    end
  end

  # The default box is an integration testing environment, installing the
  # distribution and configuring with the Puppet manifest.
  config.vm.define 'fstep', primary: true do |fstep|
    fstep.vm.box = 'centos/7'

    # Expose the container's web server on 8080
    fstep.vm.network 'forwarded_port', guest: 80, host: 8080 # apache
    fstep.vm.network 'forwarded_port', guest: 5432, host: 5432 # postgresql
    #fstep.vm.network 'forwarded_port', guest: 6565, host: 6565 # f-tep-server grpc
    #fstep.vm.network 'forwarded_port', guest: 6566, host: 6566 # f-tep-worker grpc
    #fstep.vm.network 'forwarded_port', guest: 6567, host: 6567 # f-tep-zoomanager grpc
    #fstep.vm.network 'forwarded_port', guest: 8761, host: 8761 # f-tep-serviceregistry http
    fstep.vm.network 'forwarded_port', guest: 12201, host: 12201 # graylog gelf tcp

    # Create a private network, which allows host-only access to the machine
    # using a specific IP.
    # config.vm.network "private_network", ip: "192.168.33.10"

    # Create a public network, which generally matched to bridged network.
    # Bridged networks make the machine appear as another physical device on
    # your network.
    # config.vm.network "public_network"

    # Ensure the virtualbox provider uses shared folders and not rsynced
    # folders (which may be confused by symlinks)
    fstep.vm.provider 'virtualbox' do |vb|
      fstep.vm.synced_folder '.', '/vagrant', type: 'virtualbox'
      vb.memory = 2048
      vb.cpus = 2
    end

    # Puppet provisioning
    #
    # Configure the local environment by editing distribution/puppet/hieradata/standalone.local.yaml
    #
    fstep.puppet_install.puppet_version = '4.10.4'

    # Install r10k to pull in the dependency modules
    fstep.vm.provision 'shell', inline: <<EOF
/opt/puppetlabs/puppet/bin/gem install --quiet r10k

/opt/puppetlabs/puppet/bin/r10k -v info\
  puppetfile install\
  --moduledir /tmp/vagrant-puppet/environments/puppet/modules\
  --puppetfile /tmp/vagrant-puppet/environments/puppet/Puppetfile
EOF

    # Use Vagrant's "puppet apply" provisioning
    fstep.vm.provision 'puppet' do |puppet|
      puppet.environment_path = '.dist'
      puppet.environment = 'puppet'
      puppet.hiera_config_path = '.dist/puppet/hiera.yaml'
      puppet.working_directory = '/tmp/vagrant-puppet/environments/puppet'
      # puppet.options = "--debug"
    end
  end
end
