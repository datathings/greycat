export LC_TIME=en_US.UTF-8
if [ -d "public" ]; then
  cd public
  git pull
  cd ..
else
  git clone -b gh-pages git@github.com:datathings/greycat public
fi
gitbook build
cp -R _book/* public/doc
cd public/
git add -A
MESSAGE="documentation rebuild $(date)"
git commit -m "$MESSAGE"
git push
