#!/usr/bin/env python3
"""Test the transferred binaries in a separate, offline consumer (tools already cached)."""
from pathlib import Path
import argparse
import hashlib
import os
import subprocess
import tempfile
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--bootstrap', action='store_true', help='Allow one online build to cache Gradle/Kotlin infrastructure before the offline test')
args = parser.parse_args()

root = Path(__file__).resolve().parents[1]
version = next(line.split('=', 1)[1] for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('pluginVersion='))
archive = root / f'build/distributions/plantuml-workflow-plugin-{version}.zip'
assert archive.exists(), 'Build portableBundle first'

with tempfile.TemporaryDirectory(prefix='plantuml-portable-') as temporary:
    destination = Path(temporary)
    with zipfile.ZipFile(archive) as package:
        package.extractall(destination)
    for line in (destination / 'SHA256SUMS').read_text().splitlines():
        digest, relative = line.split('  ', 1)
        assert hashlib.sha256((destination / relative).read_bytes()).hexdigest() == digest, relative
    for module, classfile in [('compiler-plugin', 'dev/benelli/CommonComponentRegistrar.class'),
                              ('gradle-plugin', 'dev/benelli/gradle/PlantUmlGeneratorGradleSubPlugin.class')]:
        jar = destination / f'repository/dev/benelli/{module}/{version}/{module}-{version}.jar'
        with zipfile.ZipFile(jar) as binary:
            assert int.from_bytes(binary.read(classfile)[6:8], 'big') == 69, 'Expected Java 25 bytecode'
            if module == 'compiler-plugin':
                for service in ['CompilerPluginRegistrar', 'CommandLineProcessor']:
                    assert binary.read('META-INF/services/org.jetbrains.kotlin.compiler.plugin.' + service)
    example = destination / 'example'
    (example / 'src/main/kotlin/Decoy.kt').write_text('''package com.decoy
interface BusinessProcess { fun steps(): List<String> }
class NotSelected : BusinessProcess { override fun steps() = listOf("ignored") }
''')

    def run(*tasks, offline=True):
        return subprocess.run(['bash', str(root / 'gradlew'), '-p', str(example),
                               *(['--offline'] if offline else []), '--console=plain', *tasks],
                              cwd=destination, text=True, capture_output=True, env=os.environ)

    if args.bootstrap:
        bootstrap = run('generateWorkflowDocumentation', offline=False)
        assert bootstrap.returncode == 0, bootstrap.stdout + bootstrap.stderr
    result = run('clean', 'generateWorkflowDocumentation', 'checkWorkflowDocumentation')
    assert result.returncode == 0, result.stdout + result.stderr
    output = example / 'build/workflow-docs/compileKotlin'
    diagram = output / 'com/acme/process/OrderProcess.puml'
    assert diagram.read_text() == '@startuml\nstart\n:ValidateOrder();\n:ShipOrder();\nstop\n@enduml\n'
    assert not (output / 'com/decoy/NotSelected.puml').exists()

    build = example / 'build.gradle.kts'
    build.write_text(build.read_text().replace('com.acme.process.BusinessProcess', ''))
    invalid = run('generateWorkflowDocumentation')
    assert invalid.returncode != 0, 'Missing interface must fail'
    assert 'fully qualified interface name' in invalid.stdout + invalid.stderr

print('PASS: checksums, Java 25 bytecode, offline binary consumer, custom interface/method, exact matching and config validation')
