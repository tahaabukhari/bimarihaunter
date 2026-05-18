.PHONY: backend-test android-build

backend-test:
	python -m pip install --upgrade pip
	pip install -r bimari-haunter/backend/requirements.txt
pytest -q bimari-haunter/backend/tests

android-build:
	cd android && chmod +x ./gradlew && ./gradlew assembleDebug
