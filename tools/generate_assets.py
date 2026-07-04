from PIL import Image, ImageDraw, ImageFilter
from pathlib import Path
import math, wave, struct, random
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'android' / 'assets'
IMG = ASSETS / 'images'
AUD = ASSETS / 'audio'
IMG.mkdir(parents=True, exist_ok=True)
AUD.mkdir(parents=True, exist_ok=True)
random.seed(12)

def save(im, name):
    im.save(IMG / name)

def grad(w,h,top,bottom):
    im=Image.new('RGBA',(w,h))
    px=im.load()
    for y in range(h):
        t=y/(h-1)
        col=tuple(int(top[i]*(1-t)+bottom[i]*t) for i in range(3))+(255,)
        for x in range(w): px[x,y]=col
    return im

def rounded(draw, box, r, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)

def draw_burak_frame(frame, phase, action):
    d=ImageDraw.Draw(frame)
    # shadow
    d.ellipse((15,54,49,62), fill=(0,0,0,55))
    leg=math.sin(phase)*5 if action=='run' else 0
    # legs
    d.rounded_rectangle((24,42,30,58+leg), radius=3, fill=(40,55,91))
    d.rounded_rectangle((35,42,41,58-leg), radius=3, fill=(36,50,82))
    # shoes
    d.ellipse((20,55+leg,31,62+leg), fill=(25,28,38))
    d.ellipse((34,55-leg,46,62-leg), fill=(25,28,38))
    # coat
    d.rounded_rectangle((18,28,47,48), radius=7, fill=(230,92,64))
    d.polygon([(18,32),(47,32),(42,51),(22,51)], fill=(207,69,61))
    d.line((22,30,43,48), fill=(255,174,76), width=2)
    # scarf
    d.rounded_rectangle((17,29,49,34), radius=3, fill=(87,183,255))
    d.rounded_rectangle((43,32,57,39), radius=3, fill=(74,157,230))
    # head
    d.ellipse((17,8,49,38), fill=(255,210,166), outline=(128,84,55), width=1)
    # hair red
    hair=[(16,19),(19,6),(35,3),(49,12),(51,24),(42,16),(33,18),(27,13),(22,21)]
    d.polygon(hair, fill=(211,65,36))
    d.ellipse((18,15,30,30), fill=(188,47,26))
    d.ellipse((39,15,51,30), fill=(188,47,26))
    # eyes
    eye_shift = 1 if action!='hurt' else 0
    d.ellipse((27+eye_shift,22,30+eye_shift,25), fill=(22,37,65))
    d.ellipse((38+eye_shift,22,41+eye_shift,25), fill=(22,37,65))
    if action=='hurt':
        d.line((29,31,38,31), fill=(22,37,65), width=1)
    else:
        d.arc((29,25,40,34), 20, 160, fill=(22,37,65), width=1)
    # hands
    d.ellipse((11,34+leg/2,20,43+leg/2), fill=(255,210,166))
    d.ellipse((45,34-leg/2,54,43-leg/2), fill=(255,210,166))
    # highlights
    d.line((23,10,31,7), fill=(255,125,70), width=2)

def make_burak():
    sheet=Image.new('RGBA',(64*8,64),(0,0,0,0))
    actions=['idle','run','run','jump','fall','hurt','interact','idle']
    for i,a in enumerate(actions):
        fr=Image.new('RGBA',(64,64),(0,0,0,0))
        draw_burak_frame(fr, i*0.9, a)
        sheet.alpha_composite(fr,(i*64,0))
    save(sheet,'burak_sheet.png')

def make_enemy():
    sheet=Image.new('RGBA',(48*4,48),(0,0,0,0))
    for i in range(4):
        im=Image.new('RGBA',(48,48),(0,0,0,0)); d=ImageDraw.Draw(im)
        d.ellipse((7,32,41,44), fill=(0,0,0,45))
        d.rounded_rectangle((8,15,40,38), radius=13, fill=(93,111,158), outline=(37,47,83), width=2)
        d.ellipse((12,6,36,24), fill=(164,190,223,230))
        d.ellipse((18,23,22,27), fill=(255,255,255))
        d.ellipse((28,23,32,27), fill=(255,255,255))
        d.ellipse((19,24,21,26), fill=(20,30,50))
        d.ellipse((29,24,31,26), fill=(20,30,50))
        off=math.sin(i*1.5)*3
        d.rounded_rectangle((12,37,20,44+off), radius=2, fill=(62,75,112))
        d.rounded_rectangle((28,37,36,44-off), radius=2, fill=(62,75,112))
        sheet.alpha_composite(im,(i*48,0))
    save(sheet,'enemy_doubt_sheet.png')

def make_tiles():
    sheet=Image.new('RGBA',(64*8,64),(0,0,0,0));
    colors=[((115,148,87),(83,107,64)),((95,113,132),(64,79,96)),((151,107,78),(97,63,40)),((101,134,186),(64,83,126)),((197,151,91),(119,79,44)),((159,64,92),(92,48,71)),((74,98,145),(44,59,95)),((143,112,184),(86,65,122))]
    for i,(top,bottom) in enumerate(colors):
        im=grad(64,64,top,bottom); d=ImageDraw.Draw(im)
        d.rounded_rectangle((0,0,63,63), radius=5, outline=(255,255,255,70), width=2)
        d.rectangle((0,0,64,15), fill=tuple(min(255,c+45) for c in top)+(255,))
        for x in range(6,64,17): d.rectangle((x,18,x+5,57), fill=(0,0,0,35))
        for _ in range(9):
            x=random.randint(5,57); y=random.randint(18,56)
            d.ellipse((x,y,x+6,y+3), fill=(255,255,255,50))
        sheet.alpha_composite(im,(i*64,0))
    save(sheet,'tileset.png')

def make_objects():
    sheet=Image.new('RGBA',(64*8,64),(0,0,0,0));
    # collectible paper plane
    for i in range(8):
        im=Image.new('RGBA',(64,64),(0,0,0,0)); d=ImageDraw.Draw(im)
        if i==0:
            d.polygon([(12,32),(54,16),(39,36)], fill=(255,255,255), outline=(207,225,255))
            d.polygon([(12,32),(39,36),(29,48)], fill=(211,230,255), outline=(207,225,255))
            d.line((54,16,29,48), fill=(168,191,235), width=2)
        elif i==1:
            d.rounded_rectangle((27,10,33,56), radius=2, fill=(255,224,155))
            d.polygon([(33,14),(56,24),(33,34)], fill=(255,191,85))
            d.ellipse((22,8,38,24), fill=(255,245,205), outline=(188,135,65))
        elif i==2:
            d.rounded_rectangle((18,10,46,58), radius=8, fill=(241,151,86), outline=(109,68,52), width=2)
            d.ellipse((39,34,45,40), fill=(255,232,158))
        elif i==3:
            d.rounded_rectangle((10,22,54,42), radius=6, fill=(197,151,91), outline=(92,54,28), width=2)
            d.rectangle((10,27,54,32), fill=(129,83,46))
        elif i==4:
            d.polygon([(8,56),(18,14),(30,56)], fill=(145,65,94)); d.polygon([(30,56),(42,14),(56,56)], fill=(167,78,108))
            d.rectangle((8,54,56,60), fill=(74,41,63))
        elif i==5:
            d.rounded_rectangle((9,17,55,47), radius=12, fill=(34,45,76), outline=(255,255,255,110), width=2)
            d.text((22,20),'▶', fill=(255,226,162))
        elif i==6:
            d.ellipse((16,16,48,48), fill=(255,214,90), outline=(120,80,20), width=2)
            d.polygon([(32,19),(36,29),(47,29),(38,35),(42,46),(32,39),(22,46),(26,35),(17,29),(28,29)], fill=(255,255,255))
        else:
            d.rounded_rectangle((9,9,55,55), radius=12, fill=(43,58,101), outline=(255,255,255,90), width=2)
        sheet.alpha_composite(im,(i*64,0))
    save(sheet,'objects.png')

def make_ui():
    im=Image.new('RGBA',(256*4,128),(0,0,0,0)); d=ImageDraw.Draw(im)
    colors=[(234,166,75),(110,181,255),(25,34,61),(184,148,255)]
    for idx,c in enumerate(colors):
        x=idx*256+18; y=28
        d.rounded_rectangle((x,y,x+220,y+72), radius=20, fill=c+(235,), outline=(255,255,255,135), width=3)
        d.rectangle((x+12,y+10,x+208,y+20), fill=(255,255,255,60))
        d.rectangle((x+12,y+55,x+208,y+64), fill=(0,0,0,35))
    save(im,'ui_atlas.png')

def make_background(idx, name, top,bottom, mood):
    im=grad(1280,720,top,bottom); d=ImageDraw.Draw(im,'RGBA')
    # moon/sun glow
    if mood in ['night','final']:
        d.ellipse((900,70,1100,270), fill=(255,224,145,42))
        d.ellipse((955,120,1030,195), fill=(255,232,160,210))
        for _ in range(90):
            x=random.randint(0,1279); y=random.randint(20,300); r=random.choice([1,1,2])
            d.ellipse((x,y,x+r,y+r), fill=(255,255,255,random.randint(80,190)))
    else:
        d.ellipse((900,60,1080,240), fill=(255,226,148,80))
        d.ellipse((955,115,1025,185), fill=(255,237,155,235))
    # far hills/city
    if mood=='city' or mood=='final':
        for x in range(-20,1300,80):
            h=random.randint(130,310)
            d.rectangle((x,520-h,x+55,520), fill=(18,26,54,180))
            for wx in range(x+8,x+50,14):
                for wy in range(530-h,500,32):
                    if random.random()<0.45: d.rectangle((wx,wy,wx+5,wy+10), fill=(255,206,90,150))
    else:
        for base,col in [(520,(42,84,88,130)),(575,(54,113,94,155)),(620,(80,129,86,170))]:
            pts=[(-50,720),(-50,base)]
            for x in range(-50,1400,120): pts.append((x,base-random.randint(30,110)))
            pts += [(1330,720)]
            d.polygon(pts, fill=col)
    # foreground silhouettes/decor
    if mood=='rain':
        for i in range(100):
            x=random.randint(0,1279); y=random.randint(0,719)
            d.line((x,y,x-12,y+28), fill=(180,210,240,75), width=2)
    if mood=='home':
        for x in range(30,1250,210):
            d.rectangle((x,470,x+130,620), fill=(118,86,68,165))
            d.polygon([(x-15,470),(x+65,405),(x+145,470)], fill=(170,82,65,190))
            d.rectangle((x+25,510,x+55,545), fill=(255,216,120,160))
    # vignette
    vign=Image.new('RGBA',(1280,720),(0,0,0,0)); vd=ImageDraw.Draw(vign)
    vd.rectangle((0,0,1280,720), outline=(0,0,0,70), width=28)
    im=Image.alpha_composite(im,vign.filter(ImageFilter.GaussianBlur(18)))
    save(im,name)

def make_backgrounds():
    make_background(0,'bg_home.png',(102,181,230),(255,205,146),'home')
    make_background(1,'bg_busroad.png',(128,199,242),(208,241,184),'road')
    make_background(2,'bg_rainvalley.png',(82,102,142),(153,169,186),'rain')
    make_background(3,'bg_city.png',(30,45,92),(79,125,189),'city')
    make_background(4,'bg_final.png',(35,26,71),(255,168,107),'final')

def wav(name, notes, dur=0.16, sr=22050, vol=0.35):
    samples=[]
    for freq,seconds in notes:
        n=int(sr*seconds)
        for i in range(n):
            t=i/sr
            env=max(0,1-i/n)
            s=math.sin(2*math.pi*freq*t)*env*vol
            samples.append(int(s*32767))
    with wave.open(str(AUD/name),'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
        w.writeframes(b''.join(struct.pack('<h',v) for v in samples))

def make_audio():
    wav('jump.wav',[(520,.08),(760,.09)],vol=.35)
    wav('collect.wav',[(880,.05),(1320,.07),(1760,.08)],vol=.32)
    wav('hurt.wav',[(220,.12),(160,.16)],vol=.36)
    wav('checkpoint.wav',[(660,.10),(990,.12),(1320,.14)],vol=.3)
    wav('win.wav',[(660,.14),(880,.14),(1320,.28)],vol=.35)
    # short loopable music/ambience motifs
    melody=[(330,.18),(392,.18),(440,.18),(392,.18),(523,.28),(440,.18),(392,.18),(330,.28)]*4
    wav('music_story.wav',melody,vol=.18)
    wav('music_level.wav',[(262,.16),(330,.16),(392,.16),(330,.16),(440,.16),(392,.16),(330,.16),(294,.16)]*8,vol=.14)
    wav('music_final.wav',[(392,.22),(494,.22),(587,.22),(659,.44),(587,.22),(494,.22),(440,.44)]*4,vol=.16)

make_burak(); make_enemy(); make_tiles(); make_objects(); make_ui(); make_backgrounds(); make_audio()
print('Generated assets in', ASSETS)
