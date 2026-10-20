#!/usr/bin/env python3
"""HTTP integration journey against a running, disposable ShopSphere database."""
import urllib.request, urllib.parse, urllib.error, http.cookiejar, re, uuid, os
BASE=os.getenv('SHOP_BASE_URL','http://localhost:8080')
class Client:
 def __init__(self): self.http=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())); self.html=''; self.url=''
 def get(self,path):
  with self.http.open(BASE+path) as r: self.html=r.read().decode(); self.url=r.url
  assert 'HTTP Status 500' not in self.html
  return self.html
 def csrf(self):
  match=re.search(r'name="csrf" value="([^"]+)"',self.html)
  assert match,'CSRF input missing'
  return match[1]
 def post(self,path,**fields):
  fields['csrf']=self.csrf()
  with self.http.open(BASE+path,urllib.parse.urlencode(fields).encode()) as r: self.html=r.read().decode(); self.url=r.url
  assert 'HTTP Status 500' not in self.html
  return self.html
 def status(self,path,expected,data=None):
  try:
   with self.http.open(BASE+path,data) as r: actual=r.status
  except urllib.error.HTTPError as e: actual=e.code
  assert actual==expected,(path,actual,expected)
checks=[]
def passed(name): checks.append(name);print('PASS',name)
c=Client();assert 'thoughtfully chosen' in c.get('/');assert '₹' in c.html and 'â' not in c.html and 'Â' not in c.html and 'à¤' not in c.html;passed('Homepage, UTF-8 and JSP rendering')
assert 'Studio Wireless Headphones' in c.get('/products?q=wireless');assert 'No finds' in c.get('/products?q=zzzzunknown');passed('Search and empty state')
c.get('/cart');assert c.url.endswith('/login');passed('Authentication redirect')
email='qa-'+uuid.uuid4().hex[:8]+'@example.com';c.get('/register');c.post('/register',name='QA Shopper',email=email,password='Testing@123');assert 'Your account is ready' in c.html;c.post('/login',email=email,password='Testing@123');assert 'QA Shopper' in c.html;passed('Registration and login')
c.status('/admin',403);c.status('/cart',403,urllib.parse.urlencode({'product':'1','quantity':'1','csrf':'invalid'}).encode());passed('Admin authorization and CSRF rejection')
c.get('/product?id=1');assert 'Studio Wireless Headphones' in c.html;c.post('/wishlist',product='1');assert 'Studio Wireless Headphones' in c.html;passed('Product details and wishlist')
c.post('/cart',product='1',quantity='1',action='add');assert '6,399.2' in c.html;c.post('/cart',product='1',quantity='2');assert '12,798.4' in c.html;passed('Cart add and quantity recalculation')
c.get('/profile');c.post('/profile',action='address',address_line='12 Test Avenue',city='Jaipur',state='Rajasthan',pincode='302001',address_type='Home');assert '12 Test Avenue' in c.html;passed('Address management')
c.get('/checkout');address=re.search(r'form="checkout-form" name="address" value="(\d+)"',c.html)[1];token=re.search(r'name="token" value="([^"]+)"',c.html)[1]
c.post('/checkout',address=address,token=token,coupon='WELCOME10',payment='MOCK_CARD',mock_result='decline');assert 'declined' in c.html;passed('Mock payment failure retains checkout')
c.post('/checkout',address=address,token=token,coupon='WELCOME10',payment='MOCK_CARD',mock_result='success');assert 'Order confirmed' in c.html;assert '11,798.4' in c.html;order=re.search(r'Order #SS-(\d+)',c.html)[1];passed('Coupon checkout, payment and confirmation')
c.get('/orders');assert '#SS-'+order in c.html;passed('Order history')
a=Client();a.get('/login');a.post('/login',email='admin@shopsphere.local',password=os.getenv('SHOP_ADMIN_PASSWORD','Admin@12345'));assert 'at a glance' in a.html;passed('Admin dashboard')
for path in ['/admin/products','/admin/products?edit=1','/admin/categories','/admin/orders','/admin/users','/admin/coupons','/demo','/demo/jsp','/demo/sql','/demo/xml']:
 a.get(path)
passed('All administration and syllabus JSPs render')
for state in ['PROCESSING','SHIPPED','DELIVERED']:
 a.get('/admin/orders');a.post('/admin/orders',id=order,status=state);assert 'Order updated' in a.html
c.get('/order?id='+order);assert 'DELIVERED' in c.html;passed('Admin order tracking visible to customer')
c.get('/product?id=1');c.post('/review',product='1',rating='5',body='A thoughtful everyday find.');assert 'Thanks for sharing' in c.html;passed('Verified purchase review')
c.get('/profile');c.post('/logout');c.get('/orders');assert c.url.endswith('/login');passed('Logout invalidates authenticated session')
c.status('/missing-page',404);passed('Custom not found page')
print(f'\n{len(checks)} HTTP journeys passed. Test customer: {email}')
