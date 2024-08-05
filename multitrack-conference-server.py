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
    #service = Service(executable_path='C:/WebDriver/chromedriver.exe') 

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


url="https://test.antmedia.io:5443/LiveApp/conference.html"
#url="www.google.com"
app = Flask(__name__)
chrome = Browser()
chrome.init(True)


@app.route('/create', methods=['GET'])
def create():
    room = request.args.get('room')
    test = request.args.get('test')
    participant = request.args.get('participant')
    print("\n create for room:"+room+":"+participant+" in "+test) 
    chrome.open_in_new_tab(url+"?roomId="+room+"&streamId="+participant, participant)
    return f'Room created', 200

@app.route('/join', methods=['GET'])
def join():
    room = request.args.get('room')
    test = request.args.get('test')
    participant = request.args.get('participant')
    print("\n join for room:"+room+":"+participant+" in "+test)
    chrome.switch_to_tab(participant) 
    join_button = chrome.get_element_by_id("join_publish_button")
    join_button.click()
    return f'Joined the room', 200

@app.route('/leave', methods=['GET'])
def leave():
    room = request.args.get('room')
    test = request.args.get('test')
    participant = request.args.get('participant')
    print("\n leave for room:"+room+":"+participant+" in "+test) 
    chrome.switch_to_tab(participant) 
    leave_button = chrome.get_element_by_id("stop_publish_button")
    leave_button.click()
    return f'Left the room', 200

@app.route('/delete', methods=['GET'])
def delete():
    room = request.args.get('room')
    test = request.args.get('test')
    participant = request.args.get('participant')
    print("\n delete for room:"+room+":"+participant+" in "+test) 
    chrome.switch_to_tab(participant) 
    chrome.close()
    return f'Tab closed', 200
   

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=3030)
