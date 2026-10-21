#!/usr/bin/env python3
"""Exercise Gradle output tracking with temporary, cross-file inherited workflows."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
fixtures = root / "app/src/main/kotlin/__lifecycle_check"
output = root / "app/build/generated/plantUml/compileKotlin/lifecycle"
parent = fixtures / "LifecycleParent.kt"
child = fixtures / "LifecycleChild.kt"


def generate():
    result = subprocess.run(
        ["bash", "gradlew", ":app:generateWorkflowDocumentation", "--console=plain"],
        cwd=root, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
    )
    if result.returncode:
        raise AssertionError(result.stdout)
    return result.stdout


if fixtures.exists():
    raise SystemExit(f"Refusing to overwrite existing directory: {fixtures}")
fixtures.mkdir()
try:
    parent.write_text("""package lifecycle
import dev.benelli.*
open class LifecycleParent : WorkflowInterface {
    override fun getActivityList(): List<Activity> = listOf(ActivityA())
}
""")
    child.write_text("package lifecycle\nclass LifecycleChild : LifecycleParent()\n")
    generate()
    diagram = output / "LifecycleChild.puml"
    assert ":ActivityA();" in diagram.read_text()
    assert ":app:compileKotlin UP-TO-DATE" in generate()

    parent.write_text(parent.read_text().replace("ActivityA()", "ActivityB()"))
    generate()
    assert ":ActivityB();" in diagram.read_text()
    assert ":ActivityA();" not in diagram.read_text()

    diagram.unlink()
    generate()
    assert ":ActivityB();" in diagram.read_text()
finally:
    parent.unlink(missing_ok=True)
    child.unlink(missing_ok=True)
    fixtures.rmdir()
    generate()

assert not (output / "LifecycleChild.puml").exists()
assert not (output / "LifecycleParent.puml").exists()
assert not (output / "LifecycleChild.diagnostics.txt").exists()
print("PASS: unchanged builds, inherited edits, missing output regeneration and stale output removal")
