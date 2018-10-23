# Food Security TEP

[FS-TEP][FS-TEP] is an open platform for the food security community to access
and exploit EO data in a collaborative virtual work environment. We're a part
of ESA's [Thematic Exploitation Platform][TEP] initiative.

## Source

The latest and greatest FS-TEP source code can be found on [GitHub][GitHub].

## Building

FS-TEP may be built and packaged using the shell scripts in the `build/`
subdirectory. The main part of the project may be built simply via Gradle.

To simplify the use of third-party dependencies in the full packaging pipeline,
we offer a Dockerfile defining the full build environment, suitable for use in
CI or locally.

To set up the build container and run the build scripts:

    docker build -t fstep-build ./build/
    docker run -v $PWD:$PWD -w $PWD fstep-build gradle build buildDist --parallel

Note that some additional paths or environment variables may be required for
each build task.

The standalone-dist.sh script produces a portable [Puppet][Puppet] environment,
using the [cgieoss-fstep][cgieoss-fstep] Puppet module (which is locally imported
to the `third-party/puppet` directory).

Vagrant may be used to manage the Docker build container:

    vagrant up build
    vagrant ssh build

## Test environment

We offer a [Vagrant][Vagrant] configuration environment which can
be used for testing the distribution locally. This requires the full build
results from running the scripts described above: `build/fstep.sh`,
`build/zoo-project.sh` and `build/standalone-dist.sh`.

Once the distribution has been prepared, create your test environment
configuration in `distribution/puppet/hieradata/standalone.local.yaml`,
for example:

    ---
    classes:
      - fstep::backend
      - fstep::db
    fstep::repo::location: 'file:///vagrant/.dist/repo'

Then install the required vagrant plugins, and bring the machine up:

    vagrant plugin install vagrant-vbguest vagrant-puppet-install
    vagrant up fstep

Vagrant will fully provision a VM from the Puppet modules and specified local
configuration. The VM's web server should be available locally on port 8080.

## License

FS-TEP is **licensed** under the [GNU Affero General Public License][AGPL]. The
terms of the license are as follows:

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

[FS-TEP]: https://foodsecurity-tep.eo.esa.int/
[TEP]: http://tep.eo.esa.int/
[Github]: https://github.com/cgi-eoss/fs-tep
[Puppet]: https://puppet.com/
[cgieoss-fstep]: https://github.com/cgi-eoss/puppet-fstep
[Vagrant]: https://vagrantup.com/
[AGPL]: https://www.gnu.org/licenses/agpl.html
