import os
from selenium import webdriver
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.webdriver.common.alert import Alert
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities
from flask import Flask, request
from selenium.webdriver.chrome.service import Service




class Browser:
  def init(self, is_headless):
    chrome_path = os.getenv('CHROME_PATH')
    chromedriver_path = os.getenv('CHROMEDRIVER_PATH')
    browser_options = Options()
    browser_options.add_experimental_option("detach", True)
    browser_options.add_argument("--use-fake-ui-for-media-stream") 
    browser_options.add_argument("--use-fake-device-for-media-stream")
    browser_options.add_argument('--log-level=3')
    browser_options.add_argument('--no-sandbox')
    browser_options.add_argument('--disable-extensions')
    browser_options.add_argument('--disable-gpu')
    browser_options.add_argument('--disable-dev-shm-usage')
    browser_options.add_argument('--disable-setuid-sandbox')
    browser_options.binary_location = chrome_path
    if is_headless:
      browser_options.add_argument("--headless")
    
    dc = DesiredCapabilities.CHROME.copy()
    dc['goog:loggingPrefs'] = { 'browser':'ALL' }

    service = Service(executable_path=chromedriver_path) 
    #service = Service(executable_path='C:/Users/yunus/Desktop/antmedia/chromedriver-win64/chromedriver.exe')


    self.driver = webdriver.Chrome(service=service, options=browser_options)

  def open_in_new_tab(self, url, tab_id):
    self.driver.switch_to.window(self.driver.window_handles[0])
    self.driver.execute_script("window.open('about:blank', '"+tab_id+"');")
    print (self.driver.window_handles)
    self.driver.switch_to.window(tab_id)
    print(url)
    self.driver.get(url)

  def get_element_by_id(self, id):
    timeout = 5
    try:
      element_present = EC.element_to_be_clickable((By.ID, id))
      WebDriverWait(self.driver, timeout).until(element_present)
    except TimeoutException:
      print ("Timed out waiting for page to load")

    element = self.driver.find_element(By.ID, id)
    return element

  def write_to_element(self, element, text):
    element.send_keys(text)

  def click_element(self, element):
    element.click()

  def switch_to_tab(self, tab_id):
    self.driver.switch_to.window(tab_id)

  def close(self):
    self.driver.close()

  def close_all(self):
    for handle in self.driver.window_handles:
      self.driver.switch_to.window(handle)
      self.driver.close()

serverUrl="https://test.antmedia.io:5443/"
appName="LiveApp/"

#url="www.google.com"
app = Flask(__name__)
chrome = Browser()
chrome.init(True)


@app.route('/createConference', methods=['GET'])
def createConference():
    print("create conference request came")
    url = serverUrl + appName + "conference.html"
    room = request.args.get('room')
    test = request.args.get('test')
    participant = request.args.get('participant')
    print("\n create for room:"+room+":"+participant+" in "+test)
    chrome.open_in_new_tab(url+"?roomId="+room+"&streamId="+participant, participant)
    return f'Room created', 200

@app.route('/joinConference', methods=['GET'])
def joinConference():
    print("join conference request came")
    room = request.args.get('room')
    test = request.args.get('test')
    participant = request.args.get('participant')
    print("\n join for room:"+room+":"+participant+" in "+test)
    chrome.switch_to_tab(participant)
    join_button = chrome.get_element_by_id("join_publish_button")
    join_button.click()
    return f'Joined the room', 200

@app.route('/leaveConference', methods=['GET'])
def leaveConference():
    print("leave request came")
    room = request.args.get('room')
    test = request.args.get('test')
    participant = request.args.get('participant')
    print("\n leave for room:"+room+":"+participant+" in "+test)
    chrome.switch_to_tab(participant)
    leave_button = chrome.get_element_by_id("stop_publish_button")
    leave_button.click()
    return f'Left the room', 200

@app.route('/deleteConference', methods=['GET'])
def deleteConference():
    print("delete conference request came")
    room = request.args.get('room')
    test = request.args.get('test')
    participant = request.args.get('participant')
    print("\n delete for room:"+room+":"+participant+" in "+test)
    chrome.switch_to_tab(participant)
    chrome.close()
    return f'Tab closed', 200

@app.route('/createP2P', methods=['GET'])
def createP2P():
    print("create p2p request came")
    url = serverUrl + appName + "peer.html"
    streamName = request.args.get('streamName')
    test = request.args.get('test')
    print("\n create p2p for streamName:"+streamName+" in "+test)
    chrome.open_in_new_tab(url, streamName)
    streamNameInput = chrome.get_element_by_id('streamName')
    streamNameInput.clear()
    streamNameInput.send_keys(streamName)

    return f'P2P created', 200

@app.route('/joinP2P', methods=['GET'])
def joinP2P():
    print("join p2p request came")
    streamName = request.args.get('streamName')
    test = request.args.get('test')
    print("\n join P2P for stream name:"+streamName+" in "+test)
    chrome.switch_to_tab(streamName)
    join_button = chrome.get_element_by_id("join_button")
    join_button.click()
    return f'Joined P2P', 200

@app.route('/leaveP2P', methods=['GET'])
def leaveP2P():
    print("leave p2p request came")
    streamName = request.args.get('streamName')
    test = request.args.get('test')
    print("\n leave P2P for stream name:"+streamName+" in "+test)
    chrome.switch_to_tab(streamName)
    leave_button = chrome.get_element_by_id("leave_button")
    leave_button.click()
    return f'Left P2P room', 200

@app.route('/deleteP2P', methods=['GET'])
def deleteP2P():
    print("delete p2p request came")
    streamName = request.args.get('streamName')
    test = request.args.get('test')
    streamName = request.args.get('streamName')
    print("\n delete P2P for stream name:"+streamName+" in "+test)
    chrome.switch_to_tab(streamName)
    chrome.close()
    return f'P2P Tab closed', 200


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=3030)
