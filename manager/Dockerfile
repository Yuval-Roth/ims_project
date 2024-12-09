FROM python:3.11

COPY src/ /manager/
COPY requirements.txt /manager/requirements.txt

RUN pip install -r /manager/requirements.txt

EXPOSE 80

ENTRYPOINT ["flask", "--app", "/manager/app", "run", "--port", "80", "--host", "0.0.0.0"]